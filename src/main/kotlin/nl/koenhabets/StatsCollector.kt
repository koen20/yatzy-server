package nl.koenhabets

import nl.koenhabets.storage.StatsDao
import java.util.*

class StatsCollector(val statsDao: StatsDao) {
    private var connected = 0
    init {
        initWsTimer()
    }

    fun setWsConnected(connected: Int) {
        this.connected = connected
    }

    private fun initWsTimer() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                statsDao.addItem(1, connected)
            }
        }, 11000, 300000)
    }
}
