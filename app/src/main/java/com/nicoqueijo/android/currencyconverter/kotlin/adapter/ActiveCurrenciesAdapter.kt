package com.nicoqueijo.android.currencyconverter.kotlin.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.nicoqueijo.android.currencyconverter.R
import com.nicoqueijo.android.currencyconverter.databinding.RowActiveCurrencyBinding
import com.nicoqueijo.android.currencyconverter.kotlin.model.Currency
import com.nicoqueijo.android.currencyconverter.kotlin.util.CurrencyDiffUtilCallback
import com.nicoqueijo.android.currencyconverter.kotlin.util.SwipeAndDragHelper
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.Order.INVALID
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.isValid
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.vibrate
import com.nicoqueijo.android.currencyconverter.kotlin.view.DecimalNumberKeyboard
import com.nicoqueijo.android.currencyconverter.kotlin.viewmodel.ActiveCurrenciesViewModel
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

class ActiveCurrenciesAdapter(private val viewModel: ActiveCurrenciesViewModel,
                              private val keyboard: DecimalNumberKeyboard) :
        ListAdapter<Currency, ActiveCurrenciesAdapter.ViewHolder>(CurrencyDiffUtilCallback()),
        SwipeAndDragHelper.ActionCompletionContract {

    private var decimalFormatter: DecimalFormat
    private var decimalSeparator: String
    private var groupingSeparator: String
    private var animBlink = AnimationUtils.loadAnimation(viewModel.getApplication(), R.anim.blink)

    init {
        val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault())
        val conversionPattern = "###,##0.0000"
        decimalFormatter = numberFormatter as DecimalFormat
        decimalFormatter.applyPattern(conversionPattern)
        groupingSeparator = decimalFormatter.decimalFormatSymbols.groupingSeparator.toString()
        decimalSeparator = decimalFormatter.decimalFormatSymbols.decimalSeparator.toString()
    }

    inner class ViewHolder(private val binding: RowActiveCurrencyBinding) :
            RecyclerView.ViewHolder(binding.root) {

        val rowForeground: ConstraintLayout = itemView.findViewById(R.id.row_foreground)
        val deleteIconStart: ImageView = itemView.findViewById(R.id.delete_icon_start)
        val deleteIconEnd: ImageView = itemView.findViewById(R.id.delete_icon_end)
        val currencyCode: TextView = itemView.findViewById(R.id.currency_code)
        private val conversion: TextView = itemView.findViewById(R.id.conversion)
        private val blinkingCursor: View = itemView.findViewById(R.id.blinking_cursor)

        init {
            conversion.setOnClickListener {
                changeFocusedCurrency(adapterPosition)
            }

            /**
             * If the [button] is a Button we know that belongs to chars 0-9 or the decimal
             * separator as those were declared as Buttons.
             * If the [button] passed in is an ImageButton that can only mean that it is the backspace
             * key as that was the only one declared as an ImageButton.
             *
             * On each key click event, we want to validate the input against what already is in the
             * TextView. If it is valid we want to run the conversion of that value against all other
             * currencies.
             */
            keyboard.onKeyClickedListener { button ->
                triggerScrollToPosition()
                val focusedCurrency = viewModel.focusedCurrency.value
                if (button is Button) {
                    val existingText = focusedCurrency?.conversion?.conversionText
                    val keyPressed = button.text
                    var input = existingText + keyPressed
                    input = cleanInput(input)
                    if (isInputValid(input)) {
                        notifyItemChanged(viewModel.adapterActiveCurrencies.indexOf(focusedCurrency))
                    } else {
                        focusedCurrency?.conversion?.hasInvalidInput = true
                        notifyItemChanged(viewModel.adapterActiveCurrencies.indexOf(focusedCurrency))
                    }
                }
                if (button is ImageButton) {
                    var existingText = focusedCurrency?.conversion?.conversionText
                    existingText = existingText?.dropLast(1)
                    focusedCurrency?.conversion?.conversionText = existingText!!
                    notifyItemChanged(viewModel.adapterActiveCurrencies.indexOf(focusedCurrency))
                }
            }

            keyboard.onKeyLongClickedListener {
                triggerScrollToPosition()
                val focusedCurrency = viewModel.focusedCurrency
                focusedCurrency.value?.conversion?.conversionText = ""
                notifyItemChanged(viewModel.adapterActiveCurrencies.indexOf(focusedCurrency.value))
            }
        }

        /**
         *  The RecyclerView scrolls to the focused position when it observes that its value has
         *  changed. This is a way to force that call.
         */
        private fun triggerScrollToPosition() {
            viewModel.focusedCurrency.value = viewModel.focusedCurrency.value
        }

        private fun cleanInput(input: String): String {
            return when (input) {
                "." -> "0$decimalSeparator"
                "00" -> "0"
                else -> input
            }
        }

        private fun isInputValid(input: String): Boolean {
            return validateLength(input) && validateDecimalPlaces(input) &&
                    validateDecimalSeparator(input) && validateZeros(input)
        }

        private fun validateLength(input: String): Boolean {
            val maxDigitsAllowed = 20
            if (!input.contains(decimalSeparator) && input.length > maxDigitsAllowed) {
                viewModel.focusedCurrency.value?.conversion?.conversionText = input.dropLast(1)
                return false
            }
            viewModel.focusedCurrency.value?.conversion?.conversionText = input
            return true
        }

        private fun validateDecimalPlaces(input: String): Boolean {
            val maxDecimalPlacesAllowed = 4
            if (input.contains(decimalSeparator) &&
                    input.substring(input.indexOf(decimalSeparator) + 1).length > maxDecimalPlacesAllowed) {
                viewModel.focusedCurrency.value?.conversion?.conversionText = input.dropLast(1)
                return false
            }
            viewModel.focusedCurrency.value?.conversion?.conversionText = input
            return true
        }

        @SuppressLint("SetTextI18n")
        private fun validateDecimalSeparator(input: String): Boolean {
            val decimalSeparatorCount = input.asSequence()
                    .count { char ->
                        char.toString() == decimalSeparator
                    }
            if (decimalSeparatorCount > 1) {
                viewModel.focusedCurrency.value?.conversion?.conversionText = input.dropLast(1)
                return false
            }
            viewModel.focusedCurrency.value?.conversion?.conversionText = input
            return true
        }

        private fun validateZeros(input: String): Boolean {
            if (input.length == 2) {
                if (input[0] == '0' && input[1] != decimalSeparator.single()) {
                    viewModel.focusedCurrency.value?.conversion?.conversionText = input[1].toString()
                    return true
                }
            }
            viewModel.focusedCurrency.value?.conversion?.conversionText = input
            return true
        }

        private fun vibrateAndShake() {
            keyboard.context.vibrate()
            conversion.startAnimation(AnimationUtils.loadAnimation(viewModel.getApplication(), R.anim.shake))
        }

        fun bind(position: Int) {
            try {
                if (viewModel.adapterActiveCurrencies[position].conversion.hasInvalidInput) {
                    vibrateAndShake()
                    viewModel.focusedCurrency.value?.conversion?.hasInvalidInput = false
                }
                binding.currency = viewModel.adapterActiveCurrencies[position]
                binding.conversion.text = viewModel.adapterActiveCurrencies[position].conversion.conversionText
                styleIfFocused()
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
                /**
                 * Create issue on Github and post link here.
                 * This error is caused by reassignFocusedCurrency() and the observe callback of
                 * the focusedCurrency in ActiveCurrenciesFragment.
                 */
            }
        }

        private fun styleIfFocused() {
            if (viewModel.adapterActiveCurrencies[adapterPosition].isFocused) {
                rowForeground.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.dark_gray))
                blinkingCursor.startAnimation(animBlink)
            } else {
                rowForeground.background = ContextCompat.getDrawable(binding.root.context,
                        R.drawable.background_row_active_currency)
                blinkingCursor.clearAnimation()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowActiveCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        setFocusToFirstCurrency()
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    /**
     * When currencies are added or removed, the upsert() function is called to modify the underlying
     * db table that stores the state of the currencies. [currencies] provides the currencies as they
     * are in the db (without the volatile fields).
     * Need to make a copy of the volatile fields and pass it back to the adapter.
     */
    fun setCurrencies(currencies: MutableList<Currency>) {
        reconcileCurrencies(currencies)
        viewModel.adapterActiveCurrencies = currencies
        submitList(currencies)
    }

    private fun reconcileCurrencies(currencies: MutableList<Currency>) {
        copyVolatileFields(currencies)
        setFocusedCurrency(currencies)
    }

    private fun setFocusedCurrency(currencies: MutableList<Currency>) {
        viewModel.focusedCurrency.value?.let {
            viewModel.focusedCurrency.value = currencies[currencies.indexOf(it)]
            currencies[currencies.indexOf(it)].isFocused = true
        }
    }

    /**
     * [currencies] will have +/- 1 elements than [adapterActiveCurrencies] due to the swiping or
     * adding event but they will have no volatile data (conversion, isFocused).
     * Goals is to copy that volatile data stored in [adapterActiveCurrencies]
     */
    private fun copyVolatileFields(currencies: MutableList<Currency>) {
        viewModel.adapterActiveCurrencies.forEach { currency ->
            if (currency.order != INVALID.position && currencies.contains(currency)) {
                currencies[currencies.indexOf(currency)] = currency
            }
        }
    }

    private fun setFocusToFirstCurrency() {
        if (viewModel.focusedCurrency.value == null) {
            viewModel.focusedCurrency.value = viewModel.adapterActiveCurrencies.take(1)[0].also { firstCurrency ->
                firstCurrency.isFocused = true
            }
        }
    }

    private fun changeFocusedCurrency(adapterPosition: Int) {
        val clickedCurrency = viewModel.adapterActiveCurrencies[adapterPosition]
        val indexOfPreviouslyFocusedCurrency = viewModel.changeFocusedCurrency(clickedCurrency)
        if (indexOfPreviouslyFocusedCurrency.isValid()) {
            notifyItemChanged(indexOfPreviouslyFocusedCurrency)
        }
        notifyItemChanged(adapterPosition)
    }

    override fun onViewMoved(oldPosition: Int, newPosition: Int) {
        viewModel.handleMove(oldPosition, newPosition)
        notifyItemMoved(oldPosition, newPosition)
    }

    override fun onViewSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int, position: Int) {
        /*val conversionValue = swipedCurrency.conversionValue*/
        viewModel.handleSwipe(position).let {
            if (it.isValid()) {
                notifyItemChanged(it)
            }
        }
        Snackbar.make(viewHolder!!.itemView, R.string.item_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    viewModel.handleSwipeUndo()
                }.show()
    }

    override fun onViewDropped() {
        viewModel.handleDrop()
    }
}