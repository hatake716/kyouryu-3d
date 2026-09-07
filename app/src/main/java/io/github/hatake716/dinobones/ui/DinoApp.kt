package io.github.hatake716.dinobones.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.hatake716.dinobones.BuildConfig
import io.github.hatake716.dinobones.data.*
import io.github.hatake716.dinobones.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

private val Ink=Color(0xFF101815)
private val Panel=Color(0xFF1C2922)
private val Sand=Color(0xFFEDE1C8)
private val Sage=Color(0xFFB6D8A5)
private val Muted=Color(0xFFA6B3AA)
private val Border=Color(0xFF35453A)
private val scheme=darkColorScheme(primary=Sage,onPrimary=Ink,background=Ink,onBackground=Sand,surface=Panel,onSurface=Sand,onSurfaceVariant=Muted,outline=Border,secondary=Color(0xFFD6B582),primaryContainer=Border,onPrimaryContainer=Sage,secondaryContainer=Border,onSecondaryContainer=Sage)
private val corner=RoundedCornerShape(20.dp)

@Composable fun DinoApp(){
    val context=LocalContext.current
    val catalog=remember {Catalog.load(context)}
    val prefs=remember{context.getSharedPreferences("collection",Context.MODE_PRIVATE)}
    var favorites by remember {mutableStateOf(prefs.getStringSet("favorites",emptySet())!!.toSet())}
    var selected by rememberSaveable{mutableStateOf<String?>(null)}
    var about by rememberSaveable{mutableStateOf(false)}
    var policy by rememberSaveable{mutableStateOf("このアプリについて")}
    fun toggle(id:String){favorites=if(id in favorites)favorites-id else favorites+id;prefs.edit().putStringSet("favorites",favorites).apply()}
    MaterialTheme(colorScheme=scheme){
        Surface(Modifier.fillMaxSize(),color=Ink){
            Box(Modifier.fillMaxSize().safeDrawingPadding(),contentAlignment=Alignment.TopCenter){
                Box(Modifier.fillMaxSize().widthIn(max=1100.dp)){
                    val d=catalog.firstOrNull{it.id==selected}
                    if(d==null)Collection(catalog,favorites,{selected=it},{policy="このアプリについて";about=true})
                    else key(d.id){Exhibit(d,d.id in favorites,{toggle(d.id)},{selected=null},{policy=it;about=true})}
                }
            }
            if(about){PolicyDialog(policy,{about=false})}
        }
    }
}
@Composable private fun SmallLabel(text:String,modifier:Modifier=Modifier){Text(text,modifier,color=Sage,fontSize=11.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.3.sp)}
@Composable private fun GlyphButton(icon:ImageVector,label:String,onClick:()->Unit){IconButton(onClick,Modifier.size(48.dp)){Icon(icon,label,tint=Sand)}}
@Composable private fun Collection(catalog:List<Dinosaur>,favorites:Set<String>,open:(String)->Unit,about:()->Unit){
    var query by rememberSaveable{mutableStateOf("")};var category by rememberSaveable{mutableStateOf("すべて")};var onlyFavorites by rememberSaveable{mutableStateOf(false)}
    val visible=catalog.filter{it.matches(query)&&(!onlyFavorites||it.id in favorites)&&when(category){"肉食恐竜"->it.category=="獣脚類";"植物食恐竜"->it.diet=="植物食";"海と空"->it.id in listOf("mosasaurus","pteranodon","archaeopteryx");else->true}}
    val grid=rememberLazyGridState()
    LazyVerticalGrid(columns=GridCells.Adaptive(160.dp),state=grid,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item(span={GridItemSpan(maxLineSpan)}){Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Outlined.ViewInAr,contentDescription=null,tint=Sage,modifier=Modifier.size(28.dp));Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)){Text("恐竜の骨格３D",fontWeight=FontWeight.Bold,fontSize=19.sp);SmallLabel("PALEO COLLECTION",Modifier.padding(top=2.dp))}
            GlyphButton(Icons.Outlined.Info,"このアプリについて",about)
        }}
        item(span={GridItemSpan(maxLineSpan)}){Column(Modifier.padding(top=14.dp,bottom=2.dp)){
            Text("太古の姿を、\nあらゆる角度から。",fontSize=30.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.8).sp)
            Text("13種類の骨格を、手のひらの展示室へ。",Modifier.padding(top=10.dp),fontSize=13.sp,color=Muted)
        }}
        if(query.isBlank()&&!onlyFavorites&&category=="すべて")item(span={GridItemSpan(maxLineSpan)}){
            val rex=catalog.first{it.id=="tyrannosaurus"}
            Surface(onClick={open(rex.id)},shape=corner,color=Panel,border=BorderStroke(1.dp,Border)){
                Column(Modifier.padding(18.dp)){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel("FEATURED EXHIBIT  /  01");Icon(Icons.Outlined.OpenInFull,null,tint=Sage,modifier=Modifier.size(17.dp))}
                    Thumbnail(rex.id,Modifier.fillMaxWidth().height(172.dp))
                    Row(verticalAlignment=Alignment.Bottom){Column(Modifier.weight(1f)){Text(rex.name,fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Tyrannosaurus rex",color=Muted,fontSize=12.sp)};Text("骨格を見る  →",fontSize=13.sp,color=Sage)}
                    Text("独自制作の学習用・模式復元",color=Muted,fontSize=10.sp,modifier=Modifier.padding(top=9.dp))
                }
            }
        }
        item(span={GridItemSpan(maxLineSpan)}){OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("名前・時代・地域で探す",fontSize=14.sp)},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(query.isNotEmpty())GlyphButton(Icons.Outlined.Close,"検索をクリア"){query=""}},singleLine=true,shape=RoundedCornerShape(16.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search))}
        item(span={GridItemSpan(maxLineSpan)}){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("すべて","肉食恐竜","植物食恐竜","海と空").forEach{FilterChip(category==it,{category=it},label={Text(it,fontSize=12.sp)})}
        }}
        item(span={GridItemSpan(maxLineSpan)}){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text("コレクション",fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.weight(1f));Text("${visible.size} / 13",color=Muted,fontSize=12.sp)
            IconToggleButton(onlyFavorites,{onlyFavorites=it}){Icon(if(onlyFavorites)Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,"お気に入りのみ",tint=if(onlyFavorites)Sage else Muted)}
        }}
        if(visible.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.SearchOff,null,tint=Muted);Text("該当する骨格がありません",Modifier.padding(top=12.dp));TextButton({query="";category="すべて";onlyFavorites=false}){Text("絞り込みを解除")}}}
        items(visible,key={it.id}){d->Surface(onClick={open(d.id)},shape=corner,color=Panel){Column(Modifier.padding(13.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel(String.format(java.util.Locale.ROOT,"%02d",catalog.indexOf(d)+1));if(d.id in favorites)Icon(Icons.Outlined.Bookmark,null,tint=Sage,modifier=Modifier.size(15.dp))}
            Thumbnail(d.id,Modifier.fillMaxWidth().height(108.dp))
            Text(d.name,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(d.latin,Modifier.padding(top=3.dp),fontSize=10.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(d.category+" · "+if(d.period.startsWith("ジュラ"))"ジュラ紀" else "白亜紀",Modifier.padding(top=10.dp),fontSize=10.sp,color=Sage)
        }}}
        item(span={GridItemSpan(maxLineSpan)}){Text("骨格は独自制作の模式復元です。実物標本のスキャンではありません。モササウルスとプテラノドンは恐竜とは異なる系統です。",Modifier.padding(top=12.dp),fontSize=11.sp,lineHeight=18.sp,color=Muted)}
        item(span={GridItemSpan(maxLineSpan)}){TextButton(about,Modifier.fillMaxWidth()){Text("出典・ライセンス・プライバシー",fontSize=12.sp)}}
    }
}
private val thumbnails=mutableMapOf<String,Bitmap>()
@Composable private fun Thumbnail(id:String,modifier:Modifier){
    val bitmap by produceState<Bitmap?>(thumbnails[id],id){if(value==null)value=withContext(Dispatchers.Default){makeThumbnail(SkeletonFactory.get(id),id in listOf("pteranodon","archaeopteryx","mosasaurus"))}.also{thumbnails[id]=it}}
    Box(modifier,contentAlignment=Alignment.Center){bitmap?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize())}?:CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=1.dp)}
}
private fun makeThumbnail(mesh:Skeleton,wide:Boolean):Bitmap {
    val bmp=Bitmap.createBitmap(780,360,Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(bmp);val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    val angle=if(wide).75f else .34f;fun proj(v:V):Pair<Float,Float> = Pair((v.x*cos(angle)-v.z*sin(angle)),v.y+(if(wide).55f else .10f)*(v.x*sin(angle)+v.z*cos(angle)))
    val points=mesh.lines.flatMap{listOf(proj(it.a),proj(it.b))}+mesh.faces.flatMap{listOf(proj(it.a),proj(it.b),proj(it.c))}
    val minX=points.minOf{it.first};val maxX=points.maxOf{it.first};val minY=points.minOf{it.second};val maxY=points.maxOf{it.second};val scale=min(710f/(maxX-minX),304f/(maxY-minY));val cx=(minX+maxX)/2;val cy=(minY+maxY)/2
    fun screen(v:V):Pair<Float,Float>{val p=proj(v);return Pair(390+(p.first-cx)*scale,180-(p.second-cy)*scale)}
    mesh.faces.forEach{f->val a=screen(f.a);val b=screen(f.b);val c=screen(f.c);val path=Path().apply{moveTo(a.first,a.second);lineTo(b.first,b.second);lineTo(c.first,c.second);close()};paint.color=0xFFBBAF93.toInt();paint.style=Paint.Style.FILL;canvas.drawPath(path,paint)}
    mesh.lines.sortedBy{it.a.z+it.b.z}.forEach{line->val a=screen(line.a);val b=screen(line.b);val width=max(1.5f,line.radius*scale*1.7f)
        paint.style=Paint.Style.STROKE;paint.color=0xFF26372D.toInt();paint.strokeWidth=width+1.3f;canvas.drawLine(a.first,a.second,b.first,b.second,paint)
        paint.color=if(line.a.z<-.1f)0xFF9D947F.toInt() else 0xFFE2D5B7.toInt();paint.strokeWidth=width;canvas.drawLine(a.first,a.second,b.first,b.second,paint)
        if(line.group in listOf(4,5,6)){paint.style=Paint.Style.FILL;canvas.drawCircle(a.first,a.second,width*.66f,paint);canvas.drawCircle(b.first,b.second,width*.6f,paint)}
    }
    return bmp
}
private class CameraMemory(var pose:CameraPose=CameraPose(),var view:SkeletonView?=null)
private val cameraSaver=Saver<CameraMemory,List<Float>>(save={val p=it.view?.museumRenderer?.pose?:it.pose;listOf(p.yaw,p.pitch,p.zoom,p.panX,p.panY)},restore={CameraMemory(CameraPose(it[0],it[1],it[2],it[3],it[4]))})

@Composable private fun Exhibit(d:Dinosaur,favorite:Boolean,toggle:()->Unit,back:()->Unit,policy:(String)->Unit){
    var tab by rememberSaveable{mutableIntStateOf(0)};var part by rememberSaveable{mutableIntStateOf(0)};var isolate by rememberSaveable{mutableStateOf(false)};var auto by rememberSaveable{mutableStateOf(false)};var fullscreen by rememberSaveable{mutableStateOf(false)}
    val compact=LocalConfiguration.current.screenHeightDp < 500
    val defaultPose=if(d.id in listOf("pteranodon","archaeopteryx","mosasaurus"))CameraPose(35f,38f) else CameraPose()
    val memory=rememberSaveable(saver=cameraSaver){CameraMemory(defaultPose)}
    var view by remember{mutableStateOf<SkeletonView?>(null)}
    BackHandler {if(fullscreen)fullscreen=false else back()}
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically){
            GlyphButton(Icons.AutoMirrored.Outlined.ArrowBack,"一覧に戻る",back)
            Column(Modifier.weight(1f)){Text(d.name,fontSize=19.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(d.latin,color=Muted,fontSize=11.sp)}
            GlyphButton(if(favorite)Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,if(favorite)"お気に入りから削除" else "お気に入りに追加",toggle)
        }
        if(!fullscreen)Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("3D骨格","詳細図鑑").forEachIndexed { index,s->FilterChip(tab==index,{tab=index},label={Text(s)},leadingIcon={Icon(if(index==0)Icons.Outlined.ViewInAr else Icons.AutoMirrored.Outlined.MenuBook,null,Modifier.size(17.dp))})}
        }
        if(tab==1&&!fullscreen){Details(d,policy);return@Column}
        val mesh by produceState<Skeleton?>(null,d.id){value=withContext(Dispatchers.Default){SkeletonFactory.get(d.id)}}
        val lifecycle=LocalLifecycleOwner.current.lifecycle
        Box(Modifier.fillMaxWidth().weight(1f)){
            AndroidView(factory={ctx->SkeletonView(ctx).also{view=it;memory.view=it;it.camera(memory.pose)}},modifier=Modifier.fillMaxSize(),update={v->mesh?.let{if(v.tag!==it){v.setModel(it);v.tag=it}};v.highlight(part,isolate);v.rotate(auto)})
            if(mesh==null)CircularProgressIndicator(Modifier.align(Alignment.Center))
            Column(Modifier.align(Alignment.TopStart).padding(20.dp)){SmallLabel("${d.category}  /  ${d.size}");Text("学習用・模式復元",Modifier.padding(top=6.dp),fontSize=11.sp,color=Muted)}
            Surface(color=Panel.copy(alpha=.92f),shape=RoundedCornerShape(14.dp),modifier=Modifier.align(if(compact)Alignment.BottomEnd else Alignment.BottomCenter).padding(end=if(compact)12.dp else 0.dp,bottom=if(compact)12.dp else 35.dp)){
                Row{GlyphButton(if(fullscreen)Icons.Outlined.CloseFullscreen else Icons.Outlined.OpenInFull,if(fullscreen)"通常表示に戻す" else "全画面で観察"){fullscreen=!fullscreen}
                    GlyphButton(Icons.Outlined.Add,"拡大"){view?.let{it.camera(it.museumRenderer.pose.scale(1.25f))}}
                    GlyphButton(Icons.Outlined.Remove,"縮小"){view?.let{it.camera(it.museumRenderer.pose.scale(.8f))}}
                    GlyphButton(Icons.Outlined.RestartAlt,"視点をリセット"){view?.camera(defaultPose);auto=false}
                }
            }
            if(!compact)Text("1本指で回転  ·  2本指で拡大・移動",Modifier.align(Alignment.BottomCenter).padding(bottom=8.dp),fontSize=11.sp,color=Muted)
        }
        DisposableEffect(view,lifecycle){
            val v=view;memory.view=v;val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->v?.onResume();Lifecycle.Event.ON_PAUSE->v?.onPause();else->Unit}}
            lifecycle.addObserver(observer);if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))v?.onResume()
            onDispose{memory.pose=v?.museumRenderer?.pose?:memory.pose;if(memory.view===v)memory.view=null;lifecycle.removeObserver(observer);v?.onPause()}
        }
        Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal=16.dp,vertical=10.dp)){
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
                listOf("側面" to CameraPose(0f,0f),"正面" to CameraPose(90f,0f),"背面" to CameraPose(-90f,0f),"上面" to CameraPose(0f,89f),"下面" to CameraPose(0f,-89f)).forEach{(name,c)->OutlinedButton({view?.camera(c);auto=false},contentPadding=PaddingValues(horizontal=13.dp,vertical=0.dp)){Text(name,fontSize=12.sp)}}
                FilterChip(auto,{auto=!auto},label={Text("自動回転",fontSize=12.sp)})
            }
            if(!fullscreen&&!compact){
                var expanded by remember{mutableStateOf(false)}
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.weight(1f)){TextButton({expanded=true}){Icon(Icons.Outlined.Layers,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("部位：${BoneGroups.names[part]}");Icon(Icons.Outlined.ExpandMore,null)}
                        DropdownMenu(expanded,{expanded=false}){BoneGroups.names.forEachIndexed{i,s->if(i==0 || mesh?.lines?.any{it.group==i}==true || mesh?.faces?.any{it.group==i}==true)DropdownMenuItem(text={Text(s)},onClick={part=i;expanded=false})}}}
                    Text("選択部位のみ",fontSize=11.sp,color=Muted);Switch(isolate,{isolate=it},modifier=Modifier.semantics{contentDescription="選択部位のみを表示"},enabled=part!=0)
                }
                Text(BoneGroups.descriptions[part],fontSize=11.sp,lineHeight=17.sp,color=Muted,maxLines=3,overflow=TextOverflow.Ellipsis)
                TextButton({policy("復元モデルについて")},contentPadding=PaddingValues(0.dp)){Text("復元の限界と出典",fontSize=11.sp)}
            }
        }
    }
}
@Composable private fun Details(d:Dinosaur,policy:(String)->Unit){
    val context=LocalContext.current
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{SmallLabel("FIELD NOTES  /  ${d.category}");Text(d.subtitle,Modifier.padding(top=10.dp),fontSize=24.sp,fontWeight=FontWeight.Bold,lineHeight=34.sp);Text(d.summary,Modifier.padding(top=12.dp),fontSize=14.sp,lineHeight=25.sp,color=Muted)}
        item{Surface(shape=corner,color=Panel){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
            Fact("生きていた時代",d.period+"\n"+d.age);Fact("化石の主な産地",d.region);Fact("大きさの目安",d.size);Fact("食性",d.diet)
        }}}
        items(d.sections.size){i->val s=d.sections[i];Column{Row(verticalAlignment=Alignment.CenterVertically){SmallLabel(String.format(java.util.Locale.ROOT,"%02d",i+1));Spacer(Modifier.width(10.dp));Text(s.title,fontSize=17.sp,fontWeight=FontWeight.SemiBold)};Text(s.text,Modifier.padding(top=9.dp),fontSize=14.sp,lineHeight=25.sp,color=Muted)}}
        item{Surface(shape=corner,color=Panel){Column(Modifier.padding(18.dp)){SmallLabel("この骨格モデルについて");Text(d.modelNote,Modifier.padding(top=10.dp),fontSize=13.sp,lineHeight=22.sp);Text("すべての骨数・関節面を正確に再現した計測資料ではありません。",Modifier.padding(top=8.dp),fontSize=12.sp,color=Muted)}}}
        item{Text("さらに詳しく",fontSize=19.sp,fontWeight=FontWeight.Bold);Text("外部アプリ・ブラウザーで開きます。閲覧には通信が必要です。",Modifier.padding(top=6.dp),fontSize=12.sp,color=Muted)
            OutlinedButton({openLink(context,d.wikipedia)},Modifier.fillMaxWidth().padding(top=10.dp)){Icon(Icons.AutoMirrored.Outlined.MenuBook,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Wikipediaで詳しく読む")}
            OutlinedButton({openLink(context,d.youtube)},Modifier.fillMaxWidth()){Icon(Icons.Outlined.PlayCircleOutline,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("YouTubeの関連動画を探す")}
            Text("YouTubeの検索結果です。特定の動画の内容・権利・正確性を保証するものではありません。",fontSize=11.sp,lineHeight=17.sp,color=Muted)}
        if(d.videos.isNotEmpty()) {
            item{Text("関連動画",fontSize=19.sp,fontWeight=FontWeight.Bold);Text("公開元で視聴できます。制作当時の復元・学説を含み、現在の図鑑解説と異なる場合があります。",Modifier.padding(top=6.dp),fontSize=12.sp,lineHeight=20.sp,color=Muted)}
            items(d.videos.size){i->val video=d.videos[i];OutlinedButton({openLink(context,video.url)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(14.dp)){Icon(Icons.Outlined.PlayCircleOutline,null,Modifier.size(20.dp));Spacer(Modifier.width(10.dp));Text(video.title,Modifier.weight(1f),fontSize=13.sp)}}
        }
        item{HorizontalDivider(color=Border);Text("出典・解説のライセンス",Modifier.padding(top=18.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
            Text("確認日 ${Catalog.REVIEW_DATE}\n解説：英語版Wikipediaの各記事を日本語で要約・再構成し、博物館資料等で補足。Wikipedia contributorsに帰属し、翻案部分はCC BY-SA 4.0で提供します。以下から原文・編集履歴を確認できます。",Modifier.padding(top=10.dp),fontSize=12.sp,lineHeight=21.sp,color=Muted)}
        items(d.sources.size){i->val source=d.sources[i];OutlinedButton({openLink(context,source.url)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(14.dp)){Text(source.title,Modifier.weight(1f),fontSize=12.sp);Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(15.dp))}}
        item{TextButton({openLink(context,"https://creativecommons.org/licenses/by-sa/4.0/deed.ja")}){Text("CC BY-SA 4.0 の条件")}
            Text("解説は確認日時点の主要情報を整理したものです。新しい研究を自動反映するものではなく、既知のすべての情報を網羅したものでもありません。推定値と復元には幅があります。",fontSize=12.sp,lineHeight=20.sp,color=Muted)}
        item{TextButton({policy("プライバシーポリシー")},Modifier.fillMaxWidth()){Text("プライバシーポリシー")}}
    }
}
@Composable private fun Fact(label:String,value:String){Row(Modifier.fillMaxWidth()){Text(label,Modifier.width(115.dp),fontSize=12.sp,color=Muted);Text(value,fontSize=13.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f))}}
internal fun openLink(context:Context,url:String){
    val uri=Uri.parse(url)
    if(uri.scheme!="https"||uri.host.isNullOrBlank())return
    try{context.startActivity(Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE))}catch(_:ActivityNotFoundException){Toast.makeText(context,"リンクを開くアプリがありません。ブラウザーをインストールしてください。",Toast.LENGTH_LONG).show()}
}
@Composable private fun PolicyDialog(initial:String,close:()->Unit){
    var page by rememberSaveable{mutableStateOf(initial)};val context=LocalContext.current
    val content=remember(page){context.assets.open(when(page){"プライバシーポリシー"->"privacy.txt";"ライセンス"->"licenses.txt";"復元モデルについて"->"models.txt";else->"about.txt"}).bufferedReader().use{it.readText()}}
    AlertDialog(onDismissRequest=close,title={Text(page)},text={Column(Modifier.verticalScroll(rememberScrollState())){
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("このアプリについて","復元モデルについて","ライセンス","プライバシーポリシー").forEach{FilterChip(page==it,{page=it},label={Text(it,fontSize=11.sp)})}}
        Text(content,fontSize=13.sp,lineHeight=22.sp)
        if(page=="ライセンス"){TextButton({openLink(context,"https://creativecommons.org/licenses/by-sa/4.0/deed.ja")}){Text("CC BY-SA 4.0 の条件")}}
        Text("Version ${BuildConfig.VERSION_NAME}",Modifier.padding(top=16.dp),fontSize=11.sp,color=Muted)
    }},confirmButton={TextButton(close){Text("閉じる")}})
}
