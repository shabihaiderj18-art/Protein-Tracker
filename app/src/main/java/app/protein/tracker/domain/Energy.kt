package app.protein.tracker.domain

import kotlin.math.roundToInt

/**
 * Daily calorie needs: resting energy (Mifflin–St Jeor) × 1.2 for normal daily life,
 * plus work and gym on top, plus the goal adjustment. Rounded to the nearest 10 kcal.
 */
object Energy {
    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int): Double =
        10 * weightKg + 6.25 * heightCm - 5 * ageYears + if (sex == Sex.MALE) 5 else -161

    fun workKcal(job: JobType, hours: Double, weightKg: Double): Double =
        ((job.met - 1.5).coerceAtLeast(0.0)) * weightKg * hours

    fun gymKcal(level: GymLevel, minutes: Int, weightKg: Double): Double =
        if (level == GymLevel.NONE) 0.0 else (level.met - 1.5) * weightKg * minutes / 60.0

    /** Null when the profile (sex, age, height, weight) isn't filled in yet. */
    fun dayTarget(settings: UserSettings, activity: ActivityInput): Double? {
        val sex = settings.sex ?: return null
        val age = settings.ageYears ?: return null
        val height = settings.heightCm ?: return null
        val weight = settings.bodyWeightKg ?: return null
        val base = bmr(sex, weight, height, age) * 1.2
        val work = if (activity.worked) workKcal(settings.jobType, activity.workHours, weight) else 0.0
        val gym = gymKcal(activity.gym, activity.gymMinutes, weight)
        val total = base + work + gym + settings.goal.kcalAdjust
        return ((total / 10).roundToInt() * 10).toDouble().coerceAtLeast(1200.0)
    }

    /** "5 ft 4 in" → 162.6 cm */
    fun feetInchesToCm(feet: Int, inches: Double): Double = (feet * 12 + inches) * 2.54
}
