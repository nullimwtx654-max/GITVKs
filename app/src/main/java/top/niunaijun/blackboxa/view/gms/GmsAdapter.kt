package top.niunaijun.blackboxa.view.gms

import android.view.View
import android.view.ViewGroup
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.GmsBean
import top.niunaijun.blackboxa.databinding.ItemGmsBinding
import top.niunaijun.blackboxa.util.toast


class GmsAdapter : RVHolderFactory() {

    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return GmsVH(inflate(R.layout.item_gms, parent))
    }

    class GmsVH(itemView: View) : RVHolder<GmsBean>(itemView) {

        private val binding = ItemGmsBinding.bind(itemView)
        override fun setContent(item: GmsBean, isSelected: Boolean, payload: Any?) {
            binding.tvTitle.text = item.userName
            binding.checkbox.isChecked = item.isInstalledGms
            binding.btnLogin.visibility = if (item.isInstalledGms) View.VISIBLE else View.GONE
            binding.btnMicroG.visibility = if (item.isInstalledGms) View.VISIBLE else View.GONE

            binding.btnLogin.setOnClickListener {
                if (!BlackBoxCore.get().launchGoogleLogin(item.userID)) {
                    toast(R.string.start_fail)
                }
            }

            binding.btnMicroG.setOnClickListener {
                if (!BlackBoxCore.get().launchMicroGSettings(item.userID)) {
                    toast(R.string.start_fail)
                }
            }

            binding.checkbox.setOnCheckedChangeListener { buttonView, _ ->
                if (buttonView.isPressed) {
                    binding.root.performClick()
                }
            }
        }
    }
}
