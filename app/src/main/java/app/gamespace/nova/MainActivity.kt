package app.gamespace.nova

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import app.gamespace.nova.translator.OverlayService
import app.gamespace.nova.translator.TranslationController
import app.gamespace.nova.performance.PerformanceEngine
import app.gamespace.nova.performance.ProfileStore

private val Lime = Color(0xFFA5FF5B)
private val Bg = Color(0xFF080908)
private val CardBg = Color(0xFF202220)
private val Muted = Color(0xFFADAFAD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { GameSpaceApp() } }
}

data class AppEntry(val label: String, val packageName: String, val icon: Drawable)

@Composable fun GameSpaceApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Início") }
    var accent by remember { mutableLongStateOf(prefs.getLong("accent", 0xFFA5FF5B).toLong()) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    val accentColor = Color(accent.toInt())
    MaterialTheme(colorScheme = darkScheme(accentColor)) {
        Scaffold(containerColor = if (dark) Bg else Color(0xFFF2F4F0), bottomBar = {
            NavigationBar(containerColor = if (dark) Color(0xFF101210) else Color.White) {
                listOf("Início" to Icons.Default.Home, "Jogos" to Icons.Default.SportsEsports, "Desempenho" to Icons.Default.Speed, "Tradutor" to Icons.Default.Translate, "Config." to Icons.Default.Tune).forEach { (name, icon) ->
                    NavigationBarItem(selected = page == name, onClick = { page = name }, icon = { Icon(icon, null) }, label = { Text(name, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, indicatorColor = accentColor.copy(alpha=.13f), selectedTextColor = accentColor))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=18.dp)) {
                when(page) {
                    "Início" -> HomePage(accentColor, { page = it })
                    "Jogos" -> GamesPage(accentColor)
                    "Desempenho" -> PerformancePage(accentColor)
                    "Tradutor" -> TranslatorPage(accentColor)
                    else -> SettingsPage(accentColor, dark, { dark = it; prefs.edit().putBoolean("dark", it).apply() }, { accent = it.toLong(); prefs.edit().putLong("accent", it.toLong()).apply() })
                }
            }
        }
    }
}

@Composable private fun darkScheme(accent: Color) = darkColorScheme(primary=accent, onPrimary=Color(0xFF14200D), background=Bg, surface=Color(0xFF151715), onSurface=Color(0xFFF4F5F3), secondary=accent)

@Composable private fun HomePage(accent: Color, navigate:(String)->Unit) {
    Spacer(Modifier.height(22.dp)); Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween, modifier=Modifier.fillMaxWidth()) {
        Column { Text("GAME SPACE", color=accent, fontSize=11.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp); Text("Jogue do seu jeito.", fontSize=27.sp, fontWeight=FontWeight.Bold) }
        Icon(Icons.Default.SportsEsports, null, tint=accent, modifier=Modifier.size(42.dp))
    }
    Spacer(Modifier.height(24.dp))
    val items = listOf(Triple("Jogos","Biblioteca personalizada",Icons.Default.SportsEsports),Triple("Desempenho","Perfis e otimizações",Icons.Default.Speed),Triple("Tradutor","Tela inteira ou região",Icons.Default.Translate),Triple("Configurações","Personalize sua experiência",Icons.Default.Tune))
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { row.forEach { (title, desc, icon) ->
            Surface(onClick={ navigate(if(title=="Configurações") "Config." else title) }, color=CardBg, shape=RoundedCornerShape(22.dp), modifier=Modifier.weight(1f).height(158.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.SpaceBetween) { Icon(icon,null,tint=accent,modifier=Modifier.size(28.dp)); Column { Text(title,fontSize=19.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(desc,color=Muted,fontSize=13.sp,lineHeight=17.sp) } }
            }
        } } }
    }
    Spacer(Modifier.height(20.dp)); Surface(color=Color(0xFF131713),shape=RoundedCornerShape(18.dp)) { Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.TipsAndUpdates,null,tint=accent); Spacer(Modifier.width(12.dp)); Text("Os ajustes avançados dependem das permissões disponíveis no Android.",fontSize=12.sp,color=Muted) } }
}

@Composable private fun GamesPage(accent:Color) {
    val ctx= LocalContext.current
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(loadApps(ctx)) }
    val saved= remember { ctx.getSharedPreferences("nova_library",Context.MODE_PRIVATE) }
    var chosen by remember { mutableStateOf(saved.getStringSet("packages", emptySet())?.toSet() ?: emptySet()) }
    var adding by remember { mutableStateOf(false) }
    var profileApp by remember { mutableStateOf<AppEntry?>(null) }
    Header("Jogos","Sua biblioteca, suas regras",accent)
    OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Buscar aplicativo")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment=Alignment.CenterVertically) { Text("${chosen.size} aplicativos",fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f)); Button(onClick={adding=true},colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color.Black)){Icon(Icons.Default.Add,null);Text("Adicionar")} }
    val selected=apps.filter { it.packageName in chosen }.filter { it.label.contains(query,true) }.sortedBy { it.label.lowercase() }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=18.dp)) { items(selected,key={it.packageName}) { app -> AppRow(app,accent,onClick={ launch(ctx,app.packageName) },onMore={profileApp=app}) } }
    if(adding) AlertDialog(onDismissRequest={adding=false},title={Text("Adicionar aplicativos")},text={Column { Text("Marque os aplicativos que deseja manter na biblioteca.",fontSize=12.sp,color=Muted); Spacer(Modifier.height(8.dp)); LazyColumn(Modifier.heightIn(max=420.dp)) { items(apps.filter { it.label.contains(query,true) }.sortedBy{it.label.lowercase()},key={it.packageName}) { app -> Row(Modifier.fillMaxWidth().clickable { chosen=if(app.packageName in chosen) chosen-app.packageName else chosen+app.packageName }.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) { Checkbox(app.packageName in chosen,{checked-> chosen=if(checked) chosen+app.packageName else chosen-app.packageName},colors=CheckboxDefaults.colors(checkedColor=accent)); Text(app.label,Modifier.weight(1f)); Text(if(app.packageName in chosen) "Adicionado" else "",fontSize=10.sp,color=Muted) } } } } },confirmButton={TextButton(onClick={saved.edit().putStringSet("packages",chosen).apply();adding=false}){Text("Concluir")} })
    profileApp?.let { app -> ProfileDialog(app,accent,{profileApp=null}) }
}

@Composable private fun AppRow(app:AppEntry,accent:Color,onClick:()->Unit,onMore:()->Unit) {
    Surface(color=CardBg,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) { AppIcon(app.icon,Modifier.size(42.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(app.label,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis); Text(app.packageName,fontSize=10.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis) }; IconButton(onClick=onMore){Icon(Icons.Default.Tune,"Perfil",tint=Muted)}; IconButton(onClick=onClick){Icon(Icons.Default.PlayArrow,"Iniciar",tint=accent)} } }
}
@Composable private fun AppIcon(drawable:Drawable,modifier:Modifier=Modifier) { val bitmap=remember(drawable) { val b=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888); val c=Canvas(b); drawable.setBounds(0,0,96,96); drawable.draw(c); b.asImageBitmap() }; Image(bitmap,null,modifier) }

@Composable private fun ProfileDialog(app:AppEntry,accent:Color,onClose:()->Unit) {
    val ctx=LocalContext.current; val store=remember { ProfileStore(ctx) }
    var width by remember { mutableStateOf(store.get(app.packageName,"width","default")) }; var height by remember { mutableStateOf(store.get(app.packageName,"height","default")) }
    var cleanup by remember { mutableStateOf(store.get(app.packageName,"cleanup","light")) }; var hz by remember { mutableStateOf(store.get(app.packageName,"hz","default")) }
    AlertDialog(onDismissRequest=onClose,title={Text("Perfil: ${app.label}")},text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Text("Resolução (largura × altura)",fontWeight=FontWeight.SemiBold); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedTextField(width,{width=it},label={Text("Largura")},modifier=Modifier.weight(1f),singleLine=true); OutlinedTextField(height,{height=it},label={Text("Altura")},modifier=Modifier.weight(1f),singleLine=true) }
        Text("Limpeza antes de iniciar",fontWeight=FontWeight.SemiBold); listOf("light" to "Leve","aggressive" to "Agressiva","custom" to "Personalizada").forEach { (v,l)-> Row(verticalAlignment=Alignment.CenterVertically){RadioButton(cleanup==v,{cleanup=v},colors=RadioButtonDefaults.colors(selectedColor=accent));Text(l)} }
        Text("Taxa de atualização desejada",fontWeight=FontWeight.SemiBold); Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("default" to "Padrão","60" to "60 Hz","90" to "90 Hz","120" to "120 Hz").forEach{(v,l)->FilterChip(hz==v,{hz=v},label={Text(l)},selected=hz==v)} }
        Text("A taxa de atualização não garante FPS do jogo. Resolução e Hz podem exigir ADB/Shizuku.",fontSize=11.sp,color=Muted)
    }},confirmButton={TextButton(onClick={store.set(app.packageName,"width",width);store.set(app.packageName,"height",height);store.set(app.packageName,"cleanup",cleanup);store.set(app.packageName,"hz",hz);onClose()}){Text("Salvar")}},dismissButton={TextButton(onClick=onClose){Text("Cancelar")}})
}

@Composable private fun PerformancePage(accent:Color) {
    val ctx=LocalContext.current; var level by remember { mutableStateOf(ctx.getSharedPreferences("performance",0).getString("level","light")?:"light") }; var whitelist by remember { mutableStateOf(ctx.getSharedPreferences("performance",0).getString("whitelist","")?:"") }; var status by remember { mutableStateOf("Pronto para configurar") }
    Header("Desempenho","Controle por perfil",accent)
    Text("Limpeza de processos",fontWeight=FontWeight.Bold,fontSize=18.sp); Spacer(Modifier.height(8.dp))
    listOf("light" to Triple("Leve","Somente tentar encerrar processos em cache",Icons.Default.BatterySaver),"aggressive" to Triple("Agressivo","Solicitar encerramento do máximo permitido",Icons.Default.Bolt),"custom" to Triple("Personalizável","Usar a lista de aplicativos protegidos",Icons.Default.Tune)).forEach{(v,item)->Surface(color=if(level==v) accent.copy(alpha=.12f) else CardBg,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().padding(vertical=4.dp).clickable { level=v;ctx.getSharedPreferences("performance",0).edit().putString("level",v).apply() }){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(item.third,null,tint=accent);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(item.first,fontWeight=FontWeight.Bold);Text(item.second,fontSize=12.sp,color=Muted)};RadioButton(level==v,{level=v},colors=RadioButtonDefaults.colors(selectedColor=accent))}}}
    if(level=="custom") { Spacer(Modifier.height(8.dp)); OutlinedTextField(whitelist,{whitelist=it;ctx.getSharedPreferences("performance",0).edit().putString("whitelist",it).apply()},label={Text("Pacotes protegidos (um por linha)")},placeholder={Text("com.android.systemui\ncom.whatsapp")},modifier=Modifier.fillMaxWidth(),minLines=3) }
    Spacer(Modifier.height(18.dp)); Text("Ferramentas",fontWeight=FontWeight.Bold,fontSize=18.sp); Spacer(Modifier.height(8.dp))
    Button(onClick={status=PerformanceEngine(ctx).clean(level,whitelist)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color.Black)){Icon(Icons.Default.CleaningServices,null);Spacer(Modifier.width(8.dp));Text("Executar limpeza agora")}
    OutlinedButton(onClick={status=PerformanceEngine(ctx).restore()},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Restore,null);Spacer(Modifier.width(8.dp));Text("Restaurar resolução padrão")}
    Surface(color=CardBg,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text(status,Modifier.padding(14.dp),fontSize=12.sp,color=Muted)}
    Text("Para comandos privilegiados, configure Shizuku/ADB nas Configurações. Sem essa autorização, o Android limita o encerramento de processos e alterações globais.",fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=20.dp))
}

@Composable private fun TranslatorPage(accent:Color) {
    val ctx=LocalContext.current; var overlay by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }; var info by remember { mutableStateOf("O tradutor usa captura de tela autorizada e modelos ML Kit.") }
    val projection= rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if(result.resultCode==Activity.RESULT_OK && result.data!=null) { TranslationController.start(ctx,result.resultCode,result.data!!); info="Serviço de captura iniciado. Use o botão flutuante." } else info="A captura de tela não foi autorizada." }
    Header("Tradutor","Tradução manual de tela",accent)
    Surface(color=CardBg,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Default.Translate,null,tint=accent,modifier=Modifier.size(34.dp));Text("Do inglês para português",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Deslize para a esquerda para traduzir a tela inteira. Deslize para a direita para selecionar uma região. Não há tradução automática em segundo plano.",fontSize=13.sp,color=Muted);Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.SwipeLeft,null,tint=accent);Text("Global",Modifier.weight(1f));Icon(Icons.Default.SwipeRight,null,tint=accent);Text("Área")}}}
    Spacer(Modifier.height(14.dp)); Button(onClick={if(!Settings.canDrawOverlays(ctx)){ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")));info="Autorize sobreposição e volte ao app."} else {projection.launch(TranslationController.captureIntent(ctx))}},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color.Black)){Icon(Icons.Default.PlayArrow,null);Spacer(Modifier.width(8.dp));Text("Ativar tradutor flutuante")}
    OutlinedButton(onClick={ctx.stopService(Intent(ctx,OverlayService::class.java));info="Solicitação de parada enviada."},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Stop,null);Spacer(Modifier.width(8.dp));Text("Parar tradutor")}
    Text(info,fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=12.dp))
    Text("O primeiro uso pode baixar os modelos de reconhecimento e tradução. O modelo inglês → português é disponibilizado pelo ML Kit sob demanda, não embutido no APK.",fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=12.dp))
}

@Composable private fun SettingsPage(accent:Color,dark:Boolean,onDark:(Boolean)->Unit,onAccent:(Int)->Unit) {
    val ctx=LocalContext.current; var shizuku by remember { mutableStateOf("Verificar status em breve") }; var overlay=Settings.canDrawOverlays(ctx)
    Header("Configurações","Personalize sua experiência",accent)
    Text("Aparência",fontWeight=FontWeight.Bold,fontSize=18.sp); Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Tema escuro");Text("Fundo AMOLED e cartões escuros",fontSize=12.sp,color=Muted)};Switch(dark,onDark,colors=SwitchDefaults.colors(checkedThumbColor=accent))}
    Text("Cor de destaque",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=10.dp,bottom=8.dp)); Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(0xFFA5FF5B.toInt(),0xFF69D7FF.toInt(),0xFFB79CFF.toInt(),0xFFFFB86B.toInt(),0xFFFF6F91.toInt(),0xFFFFFFFF.toInt()).forEach{color->Surface(color=Color(color),shape=RoundedCornerShape(50),modifier=Modifier.size(34.dp).clickable{onAccent(color)},border=if(color==accent.value.toInt()) BorderStroke(3.dp,Color.Gray) else null){}}}
    HorizontalDivider(Modifier.padding(vertical=16.dp),color=Color.Gray.copy(alpha=.25f)); Text("Permissões e sistema",fontWeight=FontWeight.Bold,fontSize=18.sp)
    SettingsAction("Permissão de sobreposição",if(overlay) "Concedida" else "Necessária",Icons.Default.Layers){ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${ctx.packageName}")))}
    SettingsAction("Opções do desenvolvedor", "ADB sem fio / depuração",Icons.Default.Terminal){ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))}
    SettingsAction("Acesso de uso", "Necessário para recursos compatíveis",Icons.Default.QueryStats){ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))}
    SettingsAction("Informações do aplicativo", "Permissões, bateria e armazenamento",Icons.Default.Info){ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${ctx.packageName}")))}
    Text("Shizuku: $shizuku",fontSize=12.sp,color=Muted,modifier=Modifier.padding(vertical=8.dp))
    Text("Privacidade: a captura de tela só começa após autorização do Android. A tradução usa o modelo ML Kit selecionado; não existe captura contínua automática.",fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=24.dp))
}

@Composable private fun SettingsAction(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,fontSize=12.sp,color=Muted)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}}
@Composable private fun Header(title:String,subtitle:String,accent:Color){Spacer(Modifier.height(22.dp));Text(title,fontSize=27.sp,fontWeight=FontWeight.Bold);Text(subtitle,fontSize=13.sp,color=Muted,modifier=Modifier.padding(top=4.dp,bottom=18.dp))}

private fun loadApps(ctx:Context):List<AppEntry>{ val pm=ctx.packageManager; val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER); val resolved=if(Build.VERSION.SDK_INT>=33) pm.queryIntentActivities(intent,PackageManager.ResolveInfoFlags.of(0)) else @Suppress("DEPRECATION") pm.queryIntentActivities(intent,0); return resolved.mapNotNull { ri -> val p=ri.activityInfo?.packageName ?: return@mapNotNull null; if(p==ctx.packageName) return@mapNotNull null; AppEntry(ri.loadLabel(pm).toString(),p,ri.loadIcon(pm)) }.distinctBy{it.packageName} }
private fun launch(ctx:Context,pkg:String){try{val i=ctx.packageManager.getLaunchIntentForPackage(pkg);if(i!=null){ctx.startActivity(i);ProfileStore(ctx).applyProfileBeforeLaunch(pkg)}else Toast.makeText(ctx,"Não foi possível abrir este aplicativo",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(ctx,e.message?:"Falha ao abrir",Toast.LENGTH_LONG).show()}}
