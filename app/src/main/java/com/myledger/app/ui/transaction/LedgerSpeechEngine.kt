package com.myledger.app.ui.transaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.*
import androidx.compose.runtime.staticCompositionLocalOf

internal interface LedgerSpeechEngine {
 val available:Boolean
 fun start(onReady:()->Unit,onLevel:(Float)->Unit,onPartial:(String)->Unit,onFinal:(String)->Unit,onError:(Int)->Unit,onEnd:()->Unit)
 fun stop()
 fun cancel()
 fun destroy()
}
internal val LocalLedgerSpeechFactory=staticCompositionLocalOf<(Context)->LedgerSpeechEngine>{{PlatformLedgerSpeechEngine(it)}}
private class PlatformLedgerSpeechEngine(private val context:Context):LedgerSpeechEngine {
 private var recognizer:SpeechRecognizer?=null
 override val available get()=SpeechRecognizer.isRecognitionAvailable(context)
 override fun start(onReady:()->Unit,onLevel:(Float)->Unit,onPartial:(String)->Unit,onFinal:(String)->Unit,onError:(Int)->Unit,onEnd:()->Unit){
  recognizer?.destroy()
  val engine=SpeechRecognizer.createSpeechRecognizer(context).also{recognizer=it}
  engine.setRecognitionListener(object:RecognitionListener {
   override fun onReadyForSpeech(p:Bundle?){onReady()}
   override fun onBeginningOfSpeech(){}
   override fun onRmsChanged(v:Float){onLevel(v)}
   override fun onBufferReceived(b:ByteArray?){}
   override fun onEndOfSpeech(){onEnd()}
   override fun onError(code:Int){onError(code)}
   override fun onResults(b:Bundle?){onFinal(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())}
   override fun onPartialResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)}
   override fun onEvent(t:Int,b:Bundle?){}
  })
  engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN").putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true).putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true))
 }
 override fun stop(){recognizer?.stopListening()}
 override fun cancel(){recognizer?.cancel()}
 override fun destroy(){recognizer?.destroy();recognizer=null}
}
