package mobile.dairy.app.services

import kotlinx.coroutines.flow.firstOrNull
import mobile.dairy.app.data.EntryDao
import mobile.dairy.app.data.FinanceDao
import mobile.dairy.app.data.GoalDao
import mobile.dairy.app.data.InsightDao
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelExporter @Inject constructor(
    private val entryDao: EntryDao,
    private val financeDao: FinanceDao,
    private val goalDao: GoalDao,
    private val insightDao: InsightDao
) {

    suspend fun exportDataToExcel(userId: String, outputStream: OutputStream) {
        val workbook: Workbook = XSSFWorkbook()

        exportJournalEntries(userId, workbook)
        exportFinances(userId, workbook)
        exportGoals(userId, workbook)

        workbook.write(outputStream)
        workbook.close()
    }

    private suspend fun exportJournalEntries(userId: String, workbook: Workbook) {
        val sheet = workbook.createSheet("Journal Entries")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("ID")
        header.createCell(1).setCellValue("Date")
        header.createCell(2).setCellValue("Moods")
        header.createCell(3).setCellValue("Note")
        
        val entries = entryDao.getAllEntries(userId).firstOrNull() ?: emptyList()
        
        entries.forEachIndexed { index, entry ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(entry.id)
            row.createCell(1).setCellValue(entry.date)
            row.createCell(2).setCellValue(entry.moods.joinToString(", "))
            row.createCell(3).setCellValue(entry.note ?: "")
        }
    }

    private suspend fun exportFinances(userId: String, workbook: Workbook) {
        val sheet = workbook.createSheet("Expenses")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("ID")
        header.createCell(1).setCellValue("Date")
        header.createCell(2).setCellValue("Amount")
        header.createCell(3).setCellValue("Category")
        header.createCell(4).setCellValue("Note")

        val expenses = financeDao.getAllExpenses(userId).firstOrNull() ?: emptyList()

        expenses.forEachIndexed { index, exp ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(exp.id)
            row.createCell(1).setCellValue(exp.date)
            row.createCell(2).setCellValue(exp.amount)
            row.createCell(3).setCellValue(exp.category)
            row.createCell(4).setCellValue(exp.note ?: "")
        }
    }

    private suspend fun exportGoals(userId: String, workbook: Workbook) {
        val sheet = workbook.createSheet("Goals")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("ID")
        header.createCell(1).setCellValue("Title")
        header.createCell(2).setCellValue("Status")
        header.createCell(3).setCellValue("Progress")

        // Wait, for goals there is no getAllGoals in DAO, I'll fetch by status or add one if needed.
        // I will use getGoalsByStatus with active and completed.
        val goals = goalDao.getGoalsByStatus(userId, listOf("active", "paused", "completed", "archived")).firstOrNull() ?: emptyList()

        goals.forEachIndexed { index, goal ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(goal.id)
            row.createCell(1).setCellValue(goal.title)
            row.createCell(2).setCellValue(goal.status)
            row.createCell(3).setCellValue(goal.progress.toDouble())
        }
    }
}
