package app.gamespace.nova.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build

class PerformanceEngine(private val context:Context) {
    fun clean(level:String,whiteListText:String):String {
        val protected=whiteListText.lines().map{it.trim()}.filter{it.isNotEmpty()}.toSet()+context.packageName
        val am=context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return try {
            if(level=="light") {
                // Android does not expose a general API to kill all cached processes. trimMemory is only for this process; report the limitation honestly.
                am.killBackgroundProcesses(context.packageName)
                "Limpeza leve solicitada. O Android limita a limpeza de processos de outros aplicativos; nenhum ganho de FPS é garantido."
            } else {
                val installed=if(Build.VERSION.SDK_INT>=33) context.packageManager.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(0)) else @Suppress("DEPRECATION") context.packageManager.getInstalledApplications(0)
                var count=0
                installed.forEach { app -> if(app.packageName !in protected && (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM)==0) { try { am.killBackgroundProcesses(app.packageName); count++ } catch (_:Exception){} } }
                "Solicitação enviada para $count apps não protegidos. O Android pode ignorar solicitações; apps do sistema foram preservados."
            }
        } catch(e:Exception){"Não foi possível executar a limpeza: ${e.message ?: "permissão insuficiente"}"}
    }
    fun restore():String = "Para restaurar a resolução via ADB/Shizuku, execute: wm size reset e wm density reset. Esta versão não altera a resolução sem autorização privilegiada."
}
