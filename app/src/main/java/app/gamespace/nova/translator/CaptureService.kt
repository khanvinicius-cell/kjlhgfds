package app.gamespace.nova.translator

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService:Service(){
    private var projection:MediaProjection?=null; private var reader:ImageReader?=null; private var display:VirtualDisplay?=null
    private val busy=AtomicBoolean(false)
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        startForeground(42,notification())
        if(intent?.action==TranslationController.ACTION_START){val code=intent.getIntExtra(TranslationController.EXTRA_RESULT,0);val data=intent.parcelable<Intent>(TranslationController.EXTRA_DATA); if(data!=null) startProjection(code,data)}
        if(intent?.action==ACTION_CAPTURE_GLOBAL) captureAndTranslate(null)
        if(intent?.action==ACTION_CAPTURE_REGION){val rect=intent.getIntArrayExtra(EXTRA_RECT);captureAndTranslate(rect)}
        return START_STICKY
    }
    private fun startProjection(code:Int,data:Intent){try{
        val mgr=getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection=mgr.getMediaProjection(code,data)
        projection?.registerCallback(object:MediaProjection.Callback(){ override fun onStop(){ display?.release(); reader?.close(); projection=null } }, Handler(Looper.getMainLooper()))
        val wm=getSystemService(WINDOW_SERVICE) as WindowManager; val metrics=DisplayMetrics();
        @Suppress("DEPRECATION") val d=wm.defaultDisplay; @Suppress("DEPRECATION") d.getRealMetrics(metrics)
        reader=ImageReader.newInstance(metrics.widthPixels,metrics.heightPixels,PixelFormat.RGBA_8888,2)
        display=projection?.createVirtualDisplay("GameSpaceTranslation",metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader?.surface,null,null)
        OverlayService.showReady(this)
    }catch(e:Exception){TranslationOverlay.showText(this,"Falha na captura: ${e.message}")}}
    fun captureAndTranslate(rect:IntArray?){ if(!busy.compareAndSet(false,true))return; try{
        val image=reader?.acquireLatestImage(); if(image==null){busy.set(false);return}
        val plane=image.planes[0];val pixel=plane.pixelStride;val row=plane.rowStride;val padding=row-pixel*image.width
        val wide=Bitmap.createBitmap(image.width+padding/pixel,image.height,Bitmap.Config.ARGB_8888);wide.copyPixelsFromBuffer(plane.buffer);image.close()
        val bmp=Bitmap.createBitmap(wide,0,0,reader!!.width,reader!!.height);wide.recycle()
        val finalBmp=if(rect!=null&&rect.size>=4){val l=rect[0].coerceIn(0,bmp.width-1);val t=rect[1].coerceIn(0,bmp.height-1);val r=rect[2].coerceIn(l+1,bmp.width);val b=rect[3].coerceIn(t+1,bmp.height);Bitmap.createBitmap(bmp,l,t,r-l,b-t).also{bmp.recycle()}}else bmp
        val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(finalBmp,0)).addOnSuccessListener { result ->
            val source=result.text.trim(); if(source.isBlank()){TranslationOverlay.showText(this,"Nenhum texto reconhecido.");finishWork(finalBmp);return@addOnSuccessListener}
            val options=TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.PORTUGUESE).build();val translator=Translation.getClient(options)
            translator.downloadModelIfNeeded().addOnSuccessListener { translator.translate(source).addOnSuccessListener { translated -> TranslationOverlay.showText(this,translated);translator.close();finishWork(finalBmp) }.addOnFailureListener{TranslationOverlay.showText(this,"Falha ao traduzir: ${it.message}");translator.close();finishWork(finalBmp)} }.addOnFailureListener{TranslationOverlay.showText(this,"Não foi possível baixar o modelo offline: ${it.message}");translator.close();finishWork(finalBmp)}
        }.addOnFailureListener{TranslationOverlay.showText(this,"Falha no OCR: ${it.message}");finishWork(finalBmp)}
    }catch(e:Exception){busy.set(false);TranslationOverlay.showText(this,"Captura falhou: ${e.message}")}}
    private fun finishWork(bitmap:Bitmap){try{bitmap.recycle()}catch(_:Exception){};busy.set(false)}
    private fun notification():Notification{val channel="nova_translate";if(Build.VERSION.SDK_INT>=26){val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel(channel,"Tradutor de tela",NotificationManager.IMPORTANCE_LOW))};return NotificationCompat.Builder(this,channel).setSmallIcon(android.R.drawable.ic_menu_search).setContentTitle("Game Space — Tradutor ativo").setContentText("A captura só ocorre quando você aciona um gesto.").setOngoing(true).build()}
    override fun onDestroy(){display?.release();reader?.close();projection?.stop();super.onDestroy()}
    companion object{const val ACTION_CAPTURE_GLOBAL="app.gamespace.nova.CAPTURE_GLOBAL";const val ACTION_CAPTURE_REGION="app.gamespace.nova.CAPTURE_REGION";const val EXTRA_RECT="rect"}
}
private inline fun <reified T:android.os.Parcelable> Intent.parcelable(key:String):T?=if(Build.VERSION.SDK_INT>=33)getParcelableExtra(key,T::class.java)else @Suppress("DEPRECATION") (getParcelableExtra(key) as? T)
