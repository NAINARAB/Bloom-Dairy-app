package mobile.dairy.app.services

import mobile.dairy.app.data.EntryDao
import mobile.dairy.app.data.FinanceDao
import mobile.dairy.app.data.GoalDao
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Goal
import mobile.dairy.app.domain.JournalEntry
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelImporter @Inject constructor(
    private val entryDao: EntryDao,
    private val financeDao: FinanceDao,
    private val goalDao: GoalDao
) {

    sealed class ImportResult {
        object Success : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    suspend fun importDataFromExcel(userId: String, inputStream: InputStream): ImportResult {
        return try {
            val workbook = WorkbookFactory.create(inputStream)
            
            // Validate all sheets first before mutating DB
            val entriesSheet = workbook.getSheet("Journal Entries")
            val expensesSheet = workbook.getSheet("Expenses")
            val goalsSheet = workbook.getSheet("Goals")

            val entries = if (entriesSheet != null) parseJournalEntries(userId, entriesSheet) else emptyList()
            val expenses = if (expensesSheet != null) parseExpenses(userId, expensesSheet) else emptyList()
            val goals = if (goalsSheet != null) parseGoals(userId, goalsSheet) else emptyList()

            // If we reach here, parsing was successful. Insert into DB.
            // In a real app we might want to do this in a single transaction,
            // but Room doesn't expose a multi-DAO transaction block easily without a generic method in AppDatabase.
            // For now, sequentially insert.
            entries.forEach { entryDao.upsertEntry(it) }
            expenses.forEach { financeDao.insertExpense(it) }
            goals.forEach { goalDao.insertGoal(it) }

            workbook.close()
            ImportResult.Success
        } catch (e: IllegalArgumentException) {
            ImportResult.Error("Validation Error: ${e.message}")
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse Excel file. Ensure the format is correct.")
        }
    }

    private fun parseJournalEntries(userId: String, sheet: org.apache.poi.ss.usermodel.Sheet): List<JournalEntry> {
        val parsed = mutableListOf<JournalEntry>()
        val rowCount = sheet.physicalNumberOfRows
        if (rowCount <= 1) return emptyList()

        for (i in 1 until rowCount) {
            val row = sheet.getRow(i) ?: continue
            
            val id = getStringCell(row, 0, "ID", i)
            val date = getStringCell(row, 1, "Date", i)
            val moodsRaw = row.getCell(2)?.stringCellValue ?: ""
            val moods = if (moodsRaw.isBlank()) emptyList() else moodsRaw.split(",").map { it.trim() }
            val note = row.getCell(3)?.stringCellValue
            
            if (id.isBlank() || date.isBlank()) {
                throw IllegalArgumentException("Row $i in Journal Entries is missing ID or Date.")
            }

            parsed.add(JournalEntry(id = id, date = date, moods = moods, note = note, userId = userId))
        }
        return parsed
    }

    private fun parseExpenses(userId: String, sheet: org.apache.poi.ss.usermodel.Sheet): List<Expense> {
        val parsed = mutableListOf<Expense>()
        val rowCount = sheet.physicalNumberOfRows
        if (rowCount <= 1) return emptyList()

        for (i in 1 until rowCount) {
            val row = sheet.getRow(i) ?: continue

            val id = getStringCell(row, 0, "ID", i)
            val date = getStringCell(row, 1, "Date", i)
            val amount = getNumericCell(row, 2, "Amount", i)
            val category = row.getCell(3)?.stringCellValue ?: "other"
            val note = row.getCell(4)?.stringCellValue

            if (id.isBlank() || date.isBlank()) {
                throw IllegalArgumentException("Row $i in Expenses is missing ID or Date.")
            }

            parsed.add(Expense(id = id, date = date, amount = amount, category = category, note = note, userId = userId))
        }
        return parsed
    }

    private fun parseGoals(userId: String, sheet: org.apache.poi.ss.usermodel.Sheet): List<Goal> {
        val parsed = mutableListOf<Goal>()
        val rowCount = sheet.physicalNumberOfRows
        if (rowCount <= 1) return emptyList()

        for (i in 1 until rowCount) {
            val row = sheet.getRow(i) ?: continue

            val id = getStringCell(row, 0, "ID", i)
            val title = getStringCell(row, 1, "Title", i)
            val status = row.getCell(2)?.stringCellValue ?: "active"
            val progress = getNumericCell(row, 3, "Progress", i).toInt()

            if (id.isBlank() || title.isBlank()) {
                throw IllegalArgumentException("Row $i in Goals is missing ID or Title.")
            }

            parsed.add(Goal(id = id, title = title, status = status, progress = progress, userId = userId))
        }
        return parsed
    }

    private fun getStringCell(row: Row, index: Int, colName: String, rowNum: Int): String {
        val cell = row.getCell(index) ?: throw IllegalArgumentException("Missing $colName at row $rowNum")
        if (cell.cellType == CellType.NUMERIC) {
            return cell.numericCellValue.toLong().toString()
        }
        return cell.stringCellValue ?: ""
    }

    private fun getNumericCell(row: Row, index: Int, colName: String, rowNum: Int): Double {
        val cell = row.getCell(index) ?: return 0.0
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue
                CellType.STRING -> cell.stringCellValue.toDouble()
                else -> 0.0
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid number format for $colName at row $rowNum")
        }
    }
}
