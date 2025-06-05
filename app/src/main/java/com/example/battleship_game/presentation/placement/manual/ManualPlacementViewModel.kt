package com.example.battleship_game.presentation.placement.manual

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.battleship_game.data.db.AppDatabase
import com.example.battleship_game.data.model.Difficulty
import com.example.battleship_game.data.model.ShipPlacement
import com.example.battleship_game.data.model.ShipPlacementUi
import com.example.battleship_game.data.repository.PlacementRepository
import kotlinx.coroutines.launch

/**
 * ViewModel для экрана ManualPlacementActivity.
 *
 * 1) Хранит список шаблонов кораблей (MutableLiveData< List<ShipPlacementUi> >).
 * 2) Хранит текущий список размещённых кораблей (MutableLiveData< List<ShipPlacementUi> >).
 * 3) При дропе нового корабля выполняет валидацию:
 *    – «палубное» пересечение (запрещено) → возвращает FAILURE → Activity вернёт корабль на станцию.
 *    – «буферное» пересечение (нефатально) → ship.isInvalid=true → допустимо, но помечаем.
 *    – Иначе → ship.isInvalid=false → допустимо.
 * 4) При повороте (double-tap) проверяет новую ориентацию аналогично.
 * 5) Поддерживает метод clearPlacement() (возвращает все корабли на станцию) и state для кнопок.
 * 6) Если пришли из экрана «Загрузка сохранённой расстановки» (fieldID >= 0), одноразово чтение из БД.
 */
class ManualPlacementViewModel(application: Application) : AndroidViewModel(application) {

    /** ID сохранённой расстановки; если -1, значит «с нуля» (нет загрузки из БД). */
    var fieldID: Long = -1L

    /** Сложность (EASY / MEDIUM / HARD) */
    var difficulty: Difficulty = Difficulty.MEDIUM

    // ======== Репозиторий для работы с БД (Room) ========

    private val repository: PlacementRepository by lazy {
        val dao = AppDatabase.getInstance(application).gamePlacementDao()
        PlacementRepository(dao)
    }

    // ======== LiveData для шаблонов и текущих размещённых кораблей ========

    /** Шаблоны (10 кораблей) для «станции» справа (они отрисованы изначально как TextView). */
    private val _templates = MutableLiveData<List<ShipPlacementUi>>(generateTemplatesUi())
    val templates: LiveData<List<ShipPlacementUi>> = _templates

    /**
     * Текущий список кораблей на поле (fromTemplate=false) или загруженных из БД.
     * Activity подписывается и передаёт его в FieldView через updateShips(...).
     */
    private val _currentPlacements = MutableLiveData<List<ShipPlacementUi>>(emptyList())
    val currentPlacements: LiveData<List<ShipPlacementUi>> = _currentPlacements

    /**
     * Живое состояние валидности расстановки: true, если все 10 кораблей находятся на поле и ни у одного isInvalid==true.
     * Activity подписывается, чтобы активировать/деактивировать кнопки «Сохранить» и «В бой!».
     */
    private val _isPlacementValid = MutableLiveData(false)
    val isPlacementValid: LiveData<Boolean> = _isPlacementValid

    /**
     * После загрузки сохранёнки один раз обновляем текущие положения кораблей.
     * Если fieldID < 0 – сразу выкладываем пустой список (значит «с нуля»).
     */
    fun loadSavedPlacement() {
        if (fieldID < 0) {
            // Нет сохранёнки – сразу пустой список
            _currentPlacements.postValue(emptyList())
            recalcValidity()
            return
        }
        viewModelScope.launch {
            // Читаем один раз из БД: List<ShipPlacement>
            val listFromDb: List<ShipPlacement> = repository.getPlacementById(fieldID)
            // Конвертируем в List<ShipPlacementUi>
            val uiList = listFromDb.map { base ->
                // fromTemplate=false, isInvalid=false (считаем, что сохранёнка корректна)
                ShipPlacementUi(
                    base = base,
                    tempX = 0f,
                    tempY = 0f,
                    fromTemplate = false,
                    isInvalid = false
                )
            }
            _currentPlacements.value = uiList
            // Удаляем те же корабли из шаблонов
            val newTemplates = _templates.value.orEmpty().toMutableList()
            uiList.forEach { placed ->
                newTemplates.removeAll { it.shipId == placed.shipId }
            }
            _templates.value = newTemplates
            recalcAllInvalidFlags()
            recalcValidity()
        }
    }

    // ======== Генерация 10 шаблонов кораблей (1×4, 2×3, 3×2, 4×1) в исходных позициях (startRow=0, startCol=0) ========

    private fun generateTemplatesUi(): List<ShipPlacementUi> {
        val list = mutableListOf<ShipPlacementUi>()
        list.add(ShipPlacementUi.from(1, 4))
        for (i in 0 until 2) list.add(ShipPlacementUi.from(2 + i, 3))
        for (i in 0 until 3) list.add(ShipPlacementUi.from(4 + i, 2))
        for (i in 0 until 4) list.add(ShipPlacementUi.from(7 + i, 1))
        return list
    }

    // ======== Обработка дропа корабля из «станции» или с поля на новое место ========

    /**
     * Пытаемся разместить [ship] на поле по ячейке [row], [col].
     * Возвращает:
     * – 0 = УСПЕШНО (без пометок)
     * – 1 = УСПЕШНО, но «буферная» коллизия → помечаем ship.isInvalid=true
     * – 2 = НЕУСПЕШНО (палубное пересечение) → ship возвращается на станцию
     */
    fun placeShip(ship: ShipPlacementUi, row: Int, col: Int): Int {
        // 1) Проверка выхода за границы
        if (!ship.isVertical && col + ship.length > GRID_SIZE) return 2
        if (ship.isVertical && row + ship.length > GRID_SIZE) return 2

        // 2) Создаём временную копию для проверки
        val tempShip = ship.copy(
            base = ShipPlacement(
                shipId = ship.shipId,
                length = ship.length,
                startRow = row,
                startCol = col,
                isVertical = ship.isVertical
            ),
            fromTemplate = false,
            isInvalid = false
        )

        // 3) Список уже стоящих (кроме этого ship)
        val occupied = _currentPlacements.value.orEmpty()
            .filter { it.shipId != ship.shipId }

        // 4) Палубная коллизия?
        if (hasDeckOverlap(tempShip, occupied)) return 2

        // 5) Обновляем currentPlacements: добавляем/заменяем ship
        val updated = _currentPlacements.value.orEmpty().toMutableList()
        if (ship.fromTemplate) {
            updated.add(tempShip)
        } else {
            updated.removeAll { it.shipId == ship.shipId }
            updated.add(tempShip)
        }
        _currentPlacements.value = updated.sortedBy { it.shipId }

        // 6) Если это был шаблон, убираем его из _templates
        if (ship.fromTemplate) {
            val newT = _templates.value.orEmpty().toMutableList()
            newT.removeAll { it.shipId == ship.shipId }
            _templates.value = newT
        }

        // 7) Пересчитываем isInvalid для всех ship (буферная коллизия)
        recalcAllInvalidFlags()

        // 8) Пересчитываем isPlacementValid
        recalcValidity()

        // 9) Определяем, стал ли именно tempShip «буферно» некорректным
        val finalList = _currentPlacements.value.orEmpty()
        val finalShip = finalList.firstOrNull { it.shipId == ship.shipId }
        return if (finalShip?.isInvalid == true) 1 else 0
    }

    /**
     * «Взять» [ship] c поля (ACTION_MOVE), но **не** возвращать его в _templates_.
     * Просто удаляем его из _currentPlacements_, чтобы он исчез с экрана и превратился в тень.
     */
    fun pickShipFromField(ship: ShipPlacementUi) {
        // 1) Удаляем ship из currentPlacements
        val updated = _currentPlacements.value.orEmpty().toMutableList()
        updated.removeAll { it.shipId == ship.shipId }
        _currentPlacements.value = updated

        // 2) Пересчитываем флаги isInvalid у остальных
        recalcAllInvalidFlags()
        // 3) Пересчитываем валидность → явно false, потому что на поле стало на 1 корабль меньше
        recalcValidity()
    }

    /**
     * Удаляет корабль [ship] из текущих размещений и возвращает его в шаблоны (станцию).
     * Используется, когда дроп произошёл «за границами» или палубная коллизия.
     */
    fun returnShipToStation(ship: ShipPlacementUi) {
        // 1) Забираем ship из currentPlacements:
        val updated = _currentPlacements.value.orEmpty().toMutableList()
        updated.removeAll { it.shipId == ship.shipId }
        _currentPlacements.value = updated

        // 2) Возвращаем в шаблоны (в исходное положение: startRow=0, startCol=0, isVertical=false, fromTemplate=true)
        val newShip = ShipPlacementUi.from(
            shipId = ship.shipId,
            length = ship.length
        )
        val newTemplates = _templates.value.orEmpty().toMutableList()
        newTemplates.add(newShip)
        _templates.value = newTemplates.sortedBy { it.length }  // чтобы было упорядоченно

        // 3) Пересчитаем валидность (скорее всего, false, т.к. не все на поле)
        recalcAllInvalidFlags()
        recalcValidity()
    }

    /**
     * Возвращает корабль [ship] именно на указанное [oldRow],[oldCol] на игровом поле.
     * Используется, когда дроп на поле привёл к «палубной» коллизии (код=2) для fromTemplate=false,
     * чтобы вернуть корабль на его прежние координаты.
     */
    fun returnShipToField(ship: ShipPlacementUi, oldRow: Int, oldCol: Int) {
        // Собираем новый экземпляр ShipPlacementUi:
        val newShip = ship.copy(
            base = ShipPlacement(
                shipId = ship.shipId,
                length = ship.length,
                startRow = oldRow,
                startCol = oldCol,
                isVertical = ship.isVertical
            ),
            fromTemplate = false,
            isInvalid = false
        )
        // 1) Удаляем любую старую версию (если она вдруг осталась)
        val updated = _currentPlacements.value.orEmpty().toMutableList()
        updated.removeAll { it.shipId == newShip.shipId }
        // 2) Добавляем обратно на поле с прежними координатами
        updated.add(newShip)
        _currentPlacements.value = updated.sortedBy { it.shipId }

        // 3) Пересчитаем флаги isInvalid и валидность
        recalcAllInvalidFlags()
        recalcValidity()
    }

    // ======== Обработка двойного тапа (поворот корабля) ========

    /**
     * Пытаемся повернуть корабль [ship] на 90° (гориз → верт или верт → гориз).
     * Возвращает:
     * – 0 = УСПЕШНО (без пометок)
     * – 1 = УСПЕШНО, но «буферная» коллизия → помечаем ship.isInvalid=true
     * – 2 = НЕУСПЕШНО (палубная коллизия) → возвращаемся к старой ориентации и вернём 2
     */
    fun rotateShip(ship: ShipPlacementUi): Int {
        val newVert = !ship.isVertical
        val row = ship.startRow
        val col = ship.startCol

        // Проверка выхода за границы при новой ориентации:
        if (newVert && row + ship.length > GRID_SIZE) return 2
        if (!newVert && col + ship.length > GRID_SIZE) return 2

        // Собираем всех, кто стоит на поле, кроме этого ship:
        val occupied = _currentPlacements.value.orEmpty().filter { it.shipId != ship.shipId }

        // Создаём временный ship для проверки
        val tempShip = ship.copy(
            base = ShipPlacement(
                shipId = ship.shipId,
                length = ship.length,
                startRow = row,
                startCol = col,
                isVertical = newVert
            ),
            fromTemplate = false,
            isInvalid = false
        )

        // 1) Проверка палубной коллизии
        if (hasDeckOverlap(tempShip, occupied)) return 2

        // 2) Обновляем currentPlacements новым положением ship
        val updated = _currentPlacements.value.orEmpty().toMutableList()
        updated.removeAll { it.shipId == ship.shipId }
        updated.add(tempShip)
        _currentPlacements.value = updated.sortedBy { it.shipId }

        // 3) Пересчитаем isInvalid для всех (буферные коллизии)
        recalcAllInvalidFlags()

        // 4) Пересчитаем валидность
        recalcValidity()

        // 5) Если tempShip в finalList оказался isInvalid=true → вернём 1; иначе 0
        val finalShip = _currentPlacements.value.orEmpty().firstOrNull { it.shipId == ship.shipId }
        return if (finalShip?.isInvalid == true) 1 else 0
    }

    // ======== Удалить все корабли с поля (вернуть на станцию) ========

    /**
     * Убирает все корабли с поля, возвращает их в шаблоны (исходное состояние).
     * После этого currentPlacements = emptyList(), templates снова содержит все 10 кораблей.
     */
    fun clearPlacement() {
        // 1) Сброс шаблонов
        val allTemplates = generateTemplatesUi()
        _templates.postValue(allTemplates)

        // 2) Сброс размещённых
        _currentPlacements.postValue(emptyList())

        // 3) Валидность = false (т.к. нет ни одного на поле)
        _isPlacementValid.postValue(false)
    }

    // ======== Валидация и вспомогательные методы ========

    /** Размер сетки = 10 (константа) */
    private val GRID_SIZE get() = 10

    /**
     * Проверяет «палубное» пересечение [candidate] с любым из [others].
     * Если хоть одна клетка «палубы» совпадает → true.
     */
    private fun hasDeckOverlap(candidate: ShipPlacementUi, others: List<ShipPlacementUi>): Boolean {
        val cells = getCells(candidate)
        others.forEach { other ->
            val oc = getCells(other)
            if (cells.any { it in oc }) return true
        }
        return false
    }

    /**
     * Возвращает список пар (row, col) для палуб (occupied cells) корабля ship.
     */
    private fun getCells(ship: ShipPlacementUi): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        val r0 = ship.startRow
        val c0 = ship.startCol
        if (ship.isVertical) {
            for (i in 0 until ship.length) list.add(r0 + i to c0)
        } else {
            for (i in 0 until ship.length) list.add(r0 to c0 + i)
        }
        return list
    }

    /**
     * Пересчитывает у _currentPlacements.isInvalid для каждого корабля:
     * true, если хоть одна палуба этого корабля попадает в буферную зону другого корабля.
     * Иначе false.
     */
    private fun recalcAllInvalidFlags() {
        val placed = _currentPlacements.value.orEmpty().toMutableList()
        val grid = GRID_SIZE

        // Сначала соберём для каждого корабля список его палуб:
        val cellsMap = placed.associateBy({ it.shipId }) { ship ->
            getCells(ship)  // List<Pair<row,col>>
        }

        // Для каждого корабля, сформируем буферную зону других:
        placed.forEach { shipA ->
            val bufferCells = mutableSetOf<Pair<Int, Int>>()
            placed.filter { it.shipId != shipA.shipId }
                .forEach { shipB ->
                    val cellsB = cellsMap[shipB.shipId] ?: emptyList()
                    cellsB.forEach { (r, c) ->
                        for (dr in -1..1) for (dc in -1..1) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until grid && nc in 0 until grid) {
                                bufferCells.add(nr to nc)
                            }
                        }
                    }
                }
            // Если хотя бы одна палуба shipA попадает в bufferCells → invalid
            val cellsA = cellsMap[shipA.shipId] ?: emptyList()
            shipA.isInvalid = cellsA.any { it in bufferCells }
        }

        // Обновляем LiveData (просто заменяем список новыми флагами)
        _currentPlacements.value = placed.sortedBy { it.shipId }
    }


    /** Пересчитывает [_isPlacementValid] → true, если ровно 10 кораблей на поле и у всех isInvalid==false */
    private fun recalcValidity() {
        val placed = _currentPlacements.value.orEmpty()
        val ok = placed.count { !it.fromTemplate && !it.isInvalid }
        _isPlacementValid.value = (placed.size == 10 && ok == 10)
    }
}