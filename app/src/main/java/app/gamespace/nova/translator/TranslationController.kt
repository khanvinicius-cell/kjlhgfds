package app.gamespace.nova.translator

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

object TranslationController {
    const val ACTION_START="app.gamespace.nova.TRANSLATION_START"
    const val EXTRA_RESULT="projection_result"
    const val EXTRA_DATA="projection_data"
    fun captureIntent(context:Context):Intent = (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()
    fun start(context:Context,resultCode:Int,data:Intent){
        val i=Intent(context,CaptureService::class.java).setAction(ACTION_START).putExtra(EXTRA_RESULT,resultCode).putExtra(EXTRA_DATA,data)
        androidx.core.content.ContextCompat.startForegroundService(context,i)
        val overlay=Intent(context,OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
        androidx.core.content.ContextCompat.startForegroundService(context,overlay)
    }
}
