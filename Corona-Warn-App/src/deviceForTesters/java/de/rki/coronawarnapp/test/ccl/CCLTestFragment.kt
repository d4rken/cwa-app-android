package de.rki.coronawarnapp.test.ccl

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.core.text.backgroundColor
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.fragment.app.Fragment
import de.rki.coronawarnapp.R
import de.rki.coronawarnapp.databinding.FragmentTestCclBinding
import de.rki.coronawarnapp.test.menu.ui.TestMenuItem
import de.rki.coronawarnapp.util.di.AutoInject
import de.rki.coronawarnapp.util.ui.observe2
import de.rki.coronawarnapp.util.ui.viewBinding
import de.rki.coronawarnapp.util.viewmodel.CWAViewModelFactoryProvider
import de.rki.coronawarnapp.util.viewmodel.cwaViewModels
import javax.inject.Inject

class CCLTestFragment : Fragment(R.layout.fragment_test_ccl), AutoInject {

    @Inject lateinit var viewModelFactory: CWAViewModelFactoryProvider.Factory

    private val viewModel: CCLTestViewModel by cwaViewModels { viewModelFactory }
    private val binding: FragmentTestCclBinding by viewBinding()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            calcDccWalletInfo.setOnClickListener { viewModel.triggerCalculation() }
            clearDccWalletInfo.setOnClickListener { viewModel.clearDccWallet() }
            viewModel.personIdentifiers.observe2(this@CCLTestFragment) { personIdentifier ->
                radioGroup.removeAllViews()
                personIdentifier.forEach { item ->
                    radioGroup.addView(
                        RadioButton(requireContext()).apply {
                            text = when (item) {
                                CCLTestViewModel.PersonIdentifierSelection.All -> "All"
                                is CCLTestViewModel.PersonIdentifierSelection.Selected -> item.personIdentifier.groupingKey
                            }
                            setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    viewModel.selectedPersonIdentifier = item
                                }
                            }
                        }
                    )
                }
                radioGroup.check(radioGroup.getChildAt(0).id)
            }

            viewModel.dccWalletInfoList.observe2(this@CCLTestFragment) { infoList ->
                val emoji = when (infoList.size) {
                    viewModel.personIdentifiers.value?.size?.minus(1) -> "✌️"
                    else -> "\uD83D\uDC4E"
                }
                infoStatus.text = "Calculation status: %s".format(emoji)
                dccWalletInfoList.text = buildSpannedString {
                    infoList.forEachIndexed { index, info ->
                        append("$index: ")
                        backgroundColor(Color.DKGRAY) {
                            color(Color.WHITE) {
                                append(info.toString())
                            }
                        }
                        appendLine()
                    }
                }
            }
        }
    }

    companion object {
        val MENU_ITEM = TestMenuItem(
            title = "CCL Configuration & WalletInfo",
            description = "Test CCL update & WalletInfo",
            targetId = R.id.cclTestFragment
        )
    }
}
