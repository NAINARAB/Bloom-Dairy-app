import json
import itertools

def generate():
    quotes = {}

    def combine(*lists):
        return [" ".join(p).strip() for p in itertools.product(*lists)]

    # 1. goals_reminder
    g_rem_intro = ["", "Hey!", "Gentle reminder:", "Just checking in.", "A quick nudge:"]
    g_rem_body = [
        "\"{goal_title}\" has been waiting quietly for {stale_days} days.",
        "It's been {stale_days} days since you last touched \"{goal_title}\".",
        "Your goal \"{goal_title}\" is still there for you.",
        "Remember \"{goal_title}\"? It's been {stale_days} days.",
        "You haven't logged time for \"{goal_title}\" in {stale_days} days."
    ]
    g_rem_action = [
        "What is one small action you can take tomorrow?",
        "Even ten minutes counts.",
        "How about picking it up today?",
        "Can you spare 5 minutes for it?",
        "Small steps lead to big wins.",
        "Don't let it fade away.",
        "A tiny step is better than no step."
    ]
    quotes["goals_reminder"] = combine(g_rem_intro, g_rem_body, g_rem_action)

    # 2. goals_streak
    g_str_intro = ["Wow!", "Incredible.", "Keep it up!", "Amazing consistency.", "You're on fire!"]
    g_str_body = [
        "You have focused on your goals for {streak} consecutive days.",
        "That's a {streak}-day goal streak.",
        "{streak} days in a row working on what matters.",
        "You've shown up for your goals {streak} days straight."
    ]
    g_str_action = [
        "Keep going.",
        "Momentum is building.",
        "You're building a powerful habit.",
        "Consistency is key.",
        "Be proud of yourself.",
        "Don't break the chain."
    ]
    quotes["goals_streak"] = combine(g_str_intro, g_str_body, g_str_action)

    # 3. goals_milestone
    g_mil_intro = ["Celebrate this:", "Milestone reached!", "Progress alert!", "Awesome news:", "Look how far you've come."]
    g_mil_body = [
        "You reached {milestone}% of \"{goal_title}\".",
        "\"{goal_title}\" is now {milestone}% complete.",
        "You've crossed the {milestone}% mark on \"{goal_title}\"."
    ]
    g_mil_action = [
        "Steady steps, real progress.",
        "Every bit counts.",
        "Keep that momentum going.",
        "You are doing great.",
        "Take a moment to appreciate your effort."
    ]
    quotes["goals_milestone"] = combine(g_mil_intro, g_mil_body, g_mil_action)

    # 4. money_avoided
    m_av_intro = ["Smart move.", "Great choice.", "Well done.", "Financial win!", "A mindful decision."]
    m_av_body = [
        "You avoided {amount} in unnecessary spending today.",
        "You kept {amount} in your pocket today.",
        "That's {amount} saved from unnecessary expenses.",
        "You successfully resisted spending {amount} today."
    ]
    m_av_action = [
        "Your future self thanks you.",
        "Small savings add up.",
        "That's how wealth is built.",
        "Keep up the discipline.",
        "Every rupee saved is a rupee earned."
    ]
    quotes["money_avoided"] = combine(m_av_intro, m_av_body, m_av_action)

    # 5. money_trend
    m_tr_intro = ["Just noticing:", "A quick heads-up:", "Something to look at:", "An observation:"]
    m_tr_body = [
        "Your {category} spending is higher than last week.",
        "You've spent more on {category} recently.",
        "There's an uptick in your {category} expenses.",
        "We noticed a spike in {category} spending."
    ]
    m_tr_action = [
        "Worth a quick look — no judgement.",
        "Might be good to review your budget.",
        "Just keeping you informed.",
        "Knowledge is power when managing money."
    ]
    quotes["money_trend"] = combine(m_tr_intro, m_tr_body, m_tr_action)

    # 6. money_streak
    m_str_intro = ["Excellent discipline.", "Great financial control.", "On track!", "Budget master."]
    m_str_body = [
        "You stayed within your daily budget for {streak} days.",
        "That's a {streak}-day budget streak.",
        "For {streak} days, you've respected your spending limits."
    ]
    m_str_action = [
        "That is real financial discipline.",
        "Keep those good habits going.",
        "You're in control of your finances.",
        "This is how you reach your financial goals."
    ]
    quotes["money_streak"] = combine(m_str_intro, m_str_body, m_str_action)

    # 7. effort_hard_day
    e_hd_intro = ["Tough day?", "It's okay.", "Rough one.", "Breathe."]
    e_hd_body = [
        "You made progress even though the day was difficult.",
        "Showing up on a hard day counts double.",
        "Difficult day, real effort.",
        "You faced a hard day and still tried."
    ]
    e_hd_action = [
        "That effort matters.",
        "Well done for trying.",
        "That combination builds strength.",
        "Rest well tonight, you earned it.",
        "Be kind to yourself."
    ]
    quotes["effort_hard_day"] = combine(e_hd_intro, e_hd_body, e_hd_action)

    # 8. effort_good_day
    e_gd_intro = ["What a day!", "Fantastic.", "Shining bright.", "A great day."]
    e_gd_body = [
        "A genuinely good day.",
        "Days like this are built by your choices.",
        "You had a highly productive and positive day.",
        "Everything seemed to click today."
    ]
    e_gd_action = [
        "Notice what made it work so you can repeat it.",
        "Enjoy it to the fullest.",
        "Let this energy carry into tomorrow.",
        "Take a moment to savor the feeling."
    ]
    quotes["effort_good_day"] = combine(e_gd_intro, e_gd_body, e_gd_action)
    
    # 9. mood_pattern_positive
    m_pos_intro = ["Insight:", "Pattern spotted:", "Interesting connection:", "Good to know:"]
    m_pos_body = [
        "\"{influencer}\" keeps showing up on your good days.",
        "You seem to feel better when \"{influencer}\" is involved.",
        "\"{influencer}\" is a strong positive influence for you."
    ]
    m_pos_action = [
        "More of that, when you can.",
        "Try to intentionally include this in your routine.",
        "Lean into what works.",
        "Keep surrounding yourself with positivity."
    ]
    quotes["mood_pattern_positive"] = combine(m_pos_intro, m_pos_body, m_pos_action)

    # 10. mood_pattern_negative
    m_neg_intro = ["Insight:", "Pattern spotted:", "Something to consider:", "Gentle observation:"]
    m_neg_body = [
        "\"{influencer}\" often appears on heavier days.",
        "We've noticed a link between difficult days and \"{influencer}\".",
        "\"{influencer}\" might be a source of friction."
    ]
    m_neg_action = [
        "Noticing the pattern is the first step — you decide what to do with it.",
        "Just being aware gives you power.",
        "Maybe take a step back and reflect on this.",
        "No pressure, just food for thought."
    ]
    quotes["mood_pattern_negative"] = combine(m_neg_intro, m_neg_body, m_neg_action)

    # 11. screen_time_high
    s_th_intro = ["Digital check:", "Screen time alert:", "A quick heads-up:", "Hey!"]
    s_th_body = [
        "Your screen time increased today ({today_mins} vs your usual {avg_mins}).",
        "You spent {today_mins} on your device, higher than your average of {avg_mins}.",
        "A bit more screen time than usual today ({today_mins})."
    ]
    s_th_action = [
        "Consider keeping your phone away during your next focus session.",
        "Maybe plan a digital detox tomorrow.",
        "Try reading a book or going for a walk instead.",
        "Unplugging for a bit could do wonders."
    ]
    quotes["screen_time_high"] = combine(s_th_intro, s_th_body, s_th_action)

    # 12. general_motivation
    gen_intro = ["Remember:", "Thought of the day:", "Just a reminder:", "Keep in mind:", "Stay strong:", "Focus:"]
    gen_body = [
        "You are capable of amazing things.",
        "Small daily improvements are the key to staggering long-term results.",
        "Your potential is endless.",
        "Every day is a second chance.",
        "Don't stop until you're proud.",
        "Focus on the step in front of you, not the whole staircase.",
        "You don't have to be perfect to be amazing.",
        "Believe you can and you're halfway there.",
        "Doubt kills more dreams than failure ever will.",
        "The best time to plant a tree was 20 years ago. The second best time is now."
    ]
    gen_action = [
        "Keep pushing forward.",
        "You've got this.",
        "Make today count.",
        "Believe in yourself.",
        "Stay disciplined.",
        "Trust the process.",
        "Be unstoppable."
    ]
    quotes["general_motivation"] = combine(gen_intro, gen_body, gen_action)
    
    # Just to add some static famous quotes to general_motivation
    famous_quotes = [
        "\"The only way to do great work is to love what you do.\" - Steve Jobs",
        "\"It does not matter how slowly you go as long as you do not stop.\" - Confucius",
        "\"Success is not final, failure is not fatal: it is the courage to continue that counts.\" - Winston Churchill",
        "\"The secret of getting ahead is getting started.\" - Mark Twain",
        "\"Don't watch the clock; do what it does. Keep going.\" - Sam Levenson",
        "\"The future belongs to those who believe in the beauty of their dreams.\" - Eleanor Roosevelt"
    ]
    quotes["general_motivation"].extend(famous_quotes)

    # Make sure we have 1000+ quotes total
    total = sum(len(v) for v in quotes.values())
    print(f"Generated {total} quotes.")

    with open('app/src/main/assets/quotes.json', 'w') as f:
        json.dump(quotes, f, indent=2)

if __name__ == '__main__':
    generate()
