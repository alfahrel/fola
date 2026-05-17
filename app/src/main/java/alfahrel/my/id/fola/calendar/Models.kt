package alfahrel.my.id.fola.calendar

data class CalendarDay(
    val day: Int,
    val isToday: Boolean,
    val isHoliday: Boolean,
    val isSunday: Boolean,
    val holidayName: String?,
    val holidayDesc: String?,
    val holidayType: HolidayType? = null,
    val isTrailing: Boolean = false
)

data class HolidayInfo(
    val day: Int,
    val month: Int,
    val year: Int,
    val name: String,
    val description: String,
    val type: HolidayType
)

data class HolidayEntry(
    val name: String,
    val description: String,
    val type: HolidayType
)

enum class HolidayType {
    NATIONAL,
    RELIGIOUS,
    JOINT_LEAVE
}