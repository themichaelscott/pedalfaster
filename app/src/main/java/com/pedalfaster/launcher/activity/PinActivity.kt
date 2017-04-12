package com.pedalfaster.launcher.activity


import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.andrognito.pinlockview.IndicatorDots
import com.andrognito.pinlockview.PinLockListener
import com.pedalfaster.launcher.R
import com.pedalfaster.launcher.dagger.Injector
import com.pedalfaster.launcher.prefs.Prefs
import kotlinx.android.synthetic.main.activity_pin.*
import pocketknife.PocketKnife
import pocketknife.SaveState
import timber.log.Timber
import javax.inject.Inject

class PinActivity : AppCompatActivity() {

    @Inject
    lateinit var prefs: Prefs

    @SaveState
    var newPin = ""
    @SaveState
    var attemptsRemaining = 3

    init {
        Injector.get().inject(this)
    }

    private val pinLockListener = object : PinLockListener {
        override fun onComplete(pin: String) { handlePin(pin) }
        override fun onEmpty() { } // do nothing
        override fun onPinChange(pinLength: Int, intermediatePin: String) { } // do nothing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        PocketKnife.restoreInstanceState(this, savedInstanceState)
        pinLockView.attachIndicatorDots(indicatorDots)
        pinLockView.setPinLockListener(pinLockListener)
        indicatorDots.indicatorType = IndicatorDots.IndicatorType.FIXED
        if (prefs.pin.isBlank()) {
            updateMessage("Create PIN")
        } else {
            updateMessage("Enter PIN")
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        PocketKnife.saveInstanceState(this, outState)
    }

    private fun resetPinFields() {
        pinLockView.resetPinLockView()
    }

    private fun updateMessage(message: String) {
        pinMessageTextView.text = message
    }

    private fun handlePin(pin: String) {
        Timber.d("Pin entered: $pin")
        when {
            prefs.pin.isNotBlank() -> {
                attemptsRemaining--
                when {
                    prefs.pin == pin -> {
                        Timber.d("PIN success")
                        updateMessage("Success")
                        setResult(SUCCESS)
                        finish()
                    }
                    attemptsRemaining > 0 -> {
                        Timber.d("PIN fail")
                        updateMessage("Please try again")
                    }
                    else -> {
                        Timber.d("Too many tries")
                        updateMessage("You have exceeded the maximum number of attempts")
                        setResult(FAIL)
                        finish()
                    }
                }
            }
            newPin.isBlank() -> {
                newPin = pin
                Timber.d("PIN1 saved")
                updateMessage("Reenter PIN")
            }
            newPin == pin -> {
                // saves the PIN
                Timber.d("new pin saved")
                prefs.pin = pin
                updateMessage("PIN Accepted")
                setResult(SUCCESS)
                finish()
            }
            else -> {
                Timber.d("pins didn't match")
                updateMessage("Your PINs did not match, please try again.\nCreate PIN")
                newPin = "" // clears the pin
            }
        }
        resetPinFields()
    }

    companion object {
        val REQUEST_CODE = 1234 // the kind of combination an idiot would have on his luggage
        val SUCCESS = 1
        val FAIL = 2
    }
}