package app.gamespace.nova.translator

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.graphics.Color
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper

object TranslationOverlay {
    private var view:TextView?=null
    fun showText(context:Context,text:String){Handler(Looper.getMainLooper()).post{
        try{val wm=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager;view?.let{try{wm.removeView(it)}catch(_:Exception){}};val v=TextView(context).apply{this.text=text;textSize=15f;setTextColor(Color.WHITE);setPadding(22,18,22,18);background=GradientDrawable().apply{setColor(0xF0222522.toInt());cornerRadius=28f;setStroke(2,0xFFA5FF5B.toInt())};elevation=12f;maxLines=10;setOnClickListener{try{wm.removeView(this)}catch(_:Exception){};view=null}}
        val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val p=WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.BOTTOM; y=100}
        wm.addView(v,p);view=v;Handler(Looper.getMainLooper()).postDelayed({try{if(view===v)wm.removeView(v);if(view===v)view=null}catch(_:Exception){}},18000)
        }catch(_:Exception){}
    }}
}
