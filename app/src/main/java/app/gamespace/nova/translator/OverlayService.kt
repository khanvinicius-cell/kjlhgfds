package app.gamespace.nova.translator

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService:Service(){
    private lateinit var wm:WindowManager; private var button:TextView?=null; private var params:WindowManager.LayoutParams?=null
    private var reposition=false; private var downX=0f;private var downY=0f;private var startX=0;private var startY=0;private var moved=false; private var downAt=0L
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{startForeground(43,notification());if(intent?.action==ACTION_SHOW)showButton();return START_STICKY}
    private fun showButton(){if(button!=null)return;wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val v=TextView(this).apply{text="文";textSize=19f;setTextColor(Color.BLACK);gravity=Gravity.CENTER;typeface=Typeface.DEFAULT_BOLD;background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.rgb(165,255,91));setStroke(2,Color.WHITE)};elevation=12f;contentDescription="Tradutor. Deslize para esquerda: global; direita: área. Toque longo: mover."}
        val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp=WindowManager.LayoutParams(dp(52),dp(52),type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=24;y=240}
        v.setOnTouchListener { view,event -> when(event.action){MotionEvent.ACTION_DOWN->{downX=event.rawX;downY=event.rawY;startX=lp.x;startY=lp.y;downAt=event.eventTime;moved=false;true}
            MotionEvent.ACTION_MOVE->{val dx=event.rawX-downX;val dy=event.rawY-downY;if(reposition&& (abs(dx)>3||abs(dy)>3)){moved=true;lp.x=startX+dx.toInt();lp.y=startY+dy.toInt();try{wm.updateViewLayout(v,lp)}catch(_:Exception){}};true}
            MotionEvent.ACTION_UP->{val dx=event.rawX-downX;val dy=event.rawY-downY;if(event.eventTime-downAt>=650){reposition=!reposition;v.alpha=if(reposition).62f else 1f;v.text=if(reposition)"↔" else "文";if(!reposition)savePosition(lp.x,lp.y)} else if(reposition){if(moved){savePosition(lp.x,lp.y)} } else if(abs(dx)>70&&abs(dx)>abs(dy)){requestCapture(if(dx<0)CaptureService.ACTION_CAPTURE_GLOBAL else CaptureService.ACTION_CAPTURE_REGION)} else if(abs(dy)>80){requestCapture(CaptureService.ACTION_CAPTURE_REGION)};true}
            MotionEvent.ACTION_CANCEL->true;else->false} }
        val saved=getSharedPreferences("overlay",0);if(saved.contains("x")){lp.x=saved.getInt("x",24);lp.y=saved.getInt("y",240)}
        try{wm.addView(v,lp);button=v;params=lp}catch(_:Exception){}
    }
    private fun requestCapture(action:String){
        if(action==CaptureService.ACTION_CAPTURE_REGION){
            RegionSelectionOverlay.begin(this) { rect ->
                try { startService(Intent(this,CaptureService::class.java).setAction(CaptureService.ACTION_CAPTURE_REGION).putExtra(CaptureService.EXTRA_RECT,rect)) }
                catch(e:Exception){TranslationOverlay.showText(this,"Não foi possível selecionar a região: ${e.message}")}
            }
        } else {
            try { startService(Intent(this,CaptureService::class.java).setAction(action)) }
            catch(e:Exception){TranslationOverlay.showText(this,"Não foi possível capturar a tela: ${e.message}")}
        }
    }
    private fun savePosition(x:Int,y:Int){getSharedPreferences("overlay",0).edit().putInt("x",x).putInt("y",y).apply()}
    private fun dp(n:Int)= (n*resources.displayMetrics.density).toInt()
    private fun notification():Notification{val c="nova_overlay";if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(c,"Botão do tradutor",NotificationManager.IMPORTANCE_LOW));return NotificationCompat.Builder(this,c).setSmallIcon(android.R.drawable.ic_menu_search).setContentTitle("Game Space — botão flutuante").setContentText("Deslize para traduzir; segure para reposicionar.").setOngoing(true).build()}
    override fun onDestroy(){try{button?.let{wm.removeView(it)}}catch(_:Exception){};button=null;super.onDestroy()}
    companion object{const val ACTION_SHOW="app.gamespace.nova.SHOW_OVERLAY";fun showReady(context:Context){TranslationOverlay.showText(context,"Tradutor pronto. Deslize no botão para traduzir.")}}
}

private object RegionSelectionOverlay {
    fun begin(context:Context, onSelected:(IntArray)->Unit) {
        try {
            val wm=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view=object:View(context){
                var sx=0f;var sy=0f;var ex=0f;var ey=0f;var selecting=false
                val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=0x55A5FF5B;style=android.graphics.Paint.Style.FILL}
                val stroke=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=0xFFA5FF5B.toInt();style=android.graphics.Paint.Style.STROKE;strokeWidth=4f}
                override fun onDraw(canvas:android.graphics.Canvas){super.onDraw(canvas);if(selecting){val r=android.graphics.RectF(minOf(sx,ex),minOf(sy,ey),maxOf(sx,ex),maxOf(sy,ey));canvas.drawRect(r,paint);canvas.drawRect(r,stroke)}else{val p=android.graphics.Paint().apply{color=0xDD080908.toInt();textSize=42f};canvas.drawText("Arraste para selecionar a área",40f,90f,p)}}
                override fun onTouchEvent(e:android.view.MotionEvent):Boolean{when(e.action){android.view.MotionEvent.ACTION_DOWN->{sx=e.rawX;sy=e.rawY;ex=sx;ey=sy;selecting=true;invalidate();return true};android.view.MotionEvent.ACTION_MOVE->{ex=e.rawX;ey=e.rawY;invalidate();return true};android.view.MotionEvent.ACTION_UP->{ex=e.rawX;ey=e.rawY;try{wm.removeView(this)}catch(_:Exception){};val l=minOf(sx,ex).toInt().coerceAtLeast(0);val t=minOf(sy,ey).toInt().coerceAtLeast(0);val r=maxOf(sx,ex).toInt();val b=maxOf(sy,ey).toInt();if(r-l>20&&b-t>20)onSelected(intArrayOf(l,t,r,b))else TranslationOverlay.showText(context,"Selecione uma região maior.");return true}};return true}
            }
            val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            val lp=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START}
            wm.addView(view,lp)
        }catch(e:Exception){TranslationOverlay.showText(context,"Permissão de sobreposição necessária para selecionar a área.")}
    }
}
