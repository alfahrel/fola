package alfahrel.my.id.fola.calendar

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MonthPagerAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 2400
    override fun createFragment(position: Int) = MonthFragment.newInstance(position)
}