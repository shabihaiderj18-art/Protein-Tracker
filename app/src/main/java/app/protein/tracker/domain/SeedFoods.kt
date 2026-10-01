package app.protein.tracker.domain

/** The foods the app starts with. All values are per 100 g (or 100 ml for milk). */
object SeedFoods {
    data class Seed(
        val name: String,
        val kcalPer100: Double,
        val proteinPer100: Double,
        val baseUnit: BaseUnit = BaseUnit.GRAM,
        val unitName: String? = null,
        val unitSize: Double? = null,
        val note: String? = null,
    )

    val all: List<Seed> = listOf(
        Seed("Chicken, cooked", 165.0, 25.0),
        Seed("Egg", 143.0, 12.6, unitName = "egg", unitSize = 50.0),
        Seed("Dahi, plain", 60.0, 3.5),
        Seed("Dahi, high-protein", 65.0, 9.0),
        Seed("Paneer", 265.0, 18.0),
        Seed("Roti", 297.0, 9.6, unitName = "roti", unitSize = 35.0),
        Seed("Pav", 280.0, 8.5, unitName = "pav", unitSize = 30.0),
        Seed("Bread", 265.0, 9.0, unitName = "slice", unitSize = 28.0),
        Seed("Haleem", 145.0, 9.0),
        Seed("Rice, cooked", 130.0, 2.7),
        Seed("Dal, cooked", 115.0, 7.0),
        Seed("Milk, full-fat", 62.0, 3.2, baseUnit = BaseUnit.ML),
        Seed("Milk, toned", 52.0, 3.2, baseUnit = BaseUnit.ML),
        Seed("Ghee", 900.0, 0.0),
        Seed("Sugar", 400.0, 0.0, unitName = "tbsp", unitSize = 12.0),
        Seed("Banana", 89.0, 1.1, unitName = "medium", unitSize = 120.0),
        Seed("Apple", 52.0, 0.3),
        Seed("Cucumber", 15.0, 0.7, unitName = "medium", unitSize = 150.0),
        Seed("Capsicum", 20.0, 0.9),
        Seed("Onion", 40.0, 1.1),
        Seed("Barfi", 450.0, 8.0, unitName = "piece", unitSize = 30.0),
        Seed("Pea protein", 390.0, 80.0, unitName = "scoop", unitSize = 30.0, note = "Scoop size varies by brand"),
    )
}
