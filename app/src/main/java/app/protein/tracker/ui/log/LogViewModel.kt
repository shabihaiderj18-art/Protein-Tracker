package app.protein.tracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.protein.tracker.data.FoodPick
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.db.Food
import app.protein.tracker.data.db.LogEntry
import app.protein.tracker.domain.MealSlot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(private val repository: ProteinRepository) : ViewModel() {

    /** All foods, or null while loading. */
    val foods: StateFlow<List<Food>?> = repository.foods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun loadEntry(id: Long): LogEntry? = repository.entry(id)

    fun saveFood(
        pick: FoodPick,
        amount: Double,
        inUnits: Boolean,
        meal: MealSlot,
        day: Long?,
        editing: LogEntry?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if (editing != null) {
                repository.updateFoodEntry(editing, amount, inUnits, meal)
            } else {
                repository.logFood(pick, amount, inUnits, meal, day)
            }
            onDone()
        }
    }

    fun saveQuick(
        name: String,
        protein: Double,
        kcal: Double,
        meal: MealSlot,
        day: Long?,
        editing: LogEntry?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if (editing != null) {
                repository.updateQuick(editing, name, protein, kcal, meal)
            } else {
                repository.logQuick(name, protein, kcal, meal, day)
            }
            onDone()
        }
    }

    fun createFood(food: Food, onCreated: (Food) -> Unit) {
        viewModelScope.launch {
            val id = repository.saveFood(food)
            onCreated(food.copy(id = id))
        }
    }

    fun setFavorite(foodId: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(foodId, favorite) }
    }
}
