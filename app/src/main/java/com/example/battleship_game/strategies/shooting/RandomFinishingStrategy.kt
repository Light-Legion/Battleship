package com.example.battleship_game.strategies.shooting

import kotlin.random.Random

/**
 * Стратегия «Случайная с добиванием».
 *
 * 1) Пока нет попаданий → выбираем любую случайную клетку из availableCells.
 * 2) Если есть попадание, мы записываем его в huntHits (список попаданий текущего корабля)
 *    и пересоздаём очередь добивания (huntQueue) через enqueueBasedOnHits().
 * 3) При потоплении вычисляем буфер вокруг всех hitCells (с помощью computeBuffer()),
 *    удаляем из availableCells сам корабль (hitCells) и весь буфер, а затем сбрасываем режим добивания.
 *
 * В любой момент стратегия НЕ вернёт клетку, которой нет в availableCells.
 */
class RandomFinishingStrategy : BaseShootingStrategy() {

    private val random = Random.Default

    /** Множество всех ещё не обстрелянных клеток. */
    private val availableCells = mutableSetOf<Pair<Int, Int>>().apply {
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                add(r to c)
            }
        }
    }

    override fun computeNextShot(): Pair<Int, Int> {
        // 1) Если очередь добивания непуста, пытаемся взять оттуда первую доступную клетку
        while (huntQueue.isNotEmpty()) {
            val cell = huntQueue.removeFirst()
            if (cell in availableCells) {
                availableCells.remove(cell)
                return cell
            }
        }

        // 2) Иначе выбираем случайную клетку из availableCells
        if (availableCells.isNotEmpty()) {
            val idx = random.nextInt(availableCells.size)
            val cell = availableCells.elementAt(idx)
            availableCells.remove(cell)
            return cell
        }

        // 3) На всякий случай (теоретически недостижимо) – возвращаем первую не tried клетку
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                if (!hasTried(r, c)) {
                    return r to c
                }
            }
        }
        throw IllegalStateException("No available cells to shoot")
    }

    override fun onShotResult(lastShot: Pair<Int, Int>?, hit: Boolean, sunk: Boolean) {
        if (lastShot == null) return
        val (r, c) = lastShot

        when {
            // ——— Потопили корабль ———
            hit && sunk -> {
                // 1) Добавляем эту клетку в huntHits, если ещё не было
                if ((r to c) !in huntHits) {
                    huntHits.add(r to c)
                }
                // 2) Считаем буфер вокруг всех палаток потопленного корабля
                val buffer = computeBuffer(huntHits)
                // 3) Удаляем из availableCells и сам корабль (huntHits), и весь буфер
                availableCells.removeAll(huntHits)
                availableCells.removeAll(buffer)
                // 4) Полный сброс добивания
                resetHuntMode()
            }

            // ——— Просто попадание (еще не потопили) ———
            hit -> {
                // 1) Сохраняем попадание
                huntHits.add(r to c)
                // 2) Перестраиваем очередь добивания
                enqueueBasedOnHits()
            }

            // ——— Промах ———
            else -> {
                // Ничего: точка уже удалена из availableCells на этапе computeNextShot.
            }
        }
    }

    /**
     * Строит «буфер» вокруг каждой клетки shipCells (8 соседей),
     * исключая сами shipCells.
     */
    private fun computeBuffer(shipCells: List<Pair<Int, Int>>): Set<Pair<Int, Int>> {
        val buffer = mutableSetOf<Pair<Int, Int>>()
        for ((r, c) in shipCells) {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0..9 && nc in 0..9) {
                        buffer.add(nr to nc)
                    }
                }
            }
        }
        buffer.removeAll(shipCells)
        return buffer
    }
}
