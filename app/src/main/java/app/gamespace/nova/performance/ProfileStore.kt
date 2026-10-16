package app.gamespace.nova.performance

import android.content.Context

class ProfileStore(context: Context) {
    private val prefs=context.getSharedPreferences("app_profiles",Context.MODE_PRIVATE)
    fun get(pkg:String,key:String,default:String)=prefs.getString("$pkg:$key",default)?:default
    fun set(pkg:String,key:String,value:String){prefs.edit().putString("$pkg:$key",value).apply()}
    fun applyProfileBeforeLaunch(pkg:String){
        val width=get(pkg,"width","default"); val height=get(pkg,"height","default")
        if(width.toIntOrNull()!=null && height.toIntOrNull()!=null) {
            // Non-root apps cannot change display size directly. The app records the profile; Shizuku integration is required to execute wm size.
        }
        // Launch-time cleanup is attempted by PerformanceEngine within Android's permitted limits.
    }
}
