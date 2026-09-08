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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.hatake716.dinobones.BuildConfig
import io.github.hatake716.dinobones.data.*
import io.github.hatake716.dinobones.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.*

internal val Ink=Color(EarthColors.Background)
internal val Panel=Color(EarthColors.Panel)
internal val Sand=Color(EarthColors.Text)
internal val Ochre=Color(EarthColors.Accent)
internal val Muted=Color(EarthColors.Muted)
internal val Border=Color(EarthColors.Border)
private val scheme=darkColorScheme(primary=Ochre,onPrimary=Ink,background=Ink,onBackground=Sand,surface=Panel,onSurface=Sand,onSurfaceVariant=Muted,outline=Border,surfaceDim=Ink,surfaceBright=Border,surfaceContainerLowest=Ink,surfaceContainerLow=Panel,surfaceContainer=Panel,surfaceContainerHigh=Color(0xFF453125),surfaceContainerHighest=Border,surfaceTint=Ochre,outlineVariant=Border,secondary=Color(0xFFD6B582),primaryContainer=Border,onPrimaryContainer=Ochre,secondaryContainer=Border,onSecondaryContainer=Ochre)
private val corner=RoundedCornerShape(20.dp)

@Composable fun DinoApp(){
    val context=LocalContext.current
    val screenState=rememberSaveableStateHolder()
    val catalog=remember {Catalog.load(context)}
    val prefs=remember{context.getSharedPreferences("collection",Context.MODE_PRIVATE)}
    var favorites by remember {mutableStateOf(prefs.getStringSet("favorites",emptySet())!!.toSet())}
    var homeMode by rememberSaveable{mutableStateOf("theater")}
    val collectionAllPeriods=rememberSaveable{mutableStateOf(false)}
    var fromTheater by rememberSaveable{mutableStateOf(false)}
    var selected by rememberSaveable{mutableStateOf<String?>(null)}
    var about by rememberSaveable{mutableStateOf(false)}
    var policy by rememberSaveable{mutableStateOf("このアプリについて")}
    var artworkInfo by rememberSaveable{mutableStateOf(false)}
    var backgroundStrength by rememberSaveable{mutableFloatStateOf(.32f)}
    var backgroundFill by rememberSaveable{mutableStateOf(true)}
    var backgroundFrame by remember{mutableStateOf(Rect.Zero)}
    val current=catalog.firstOrNull{it.id==selected}
    val fullTheater=LocalConfiguration.current.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE&&homeMode=="theater"&&current==null
    val artwork by produceState<Bitmap?>(null,current?.artwork?.asset){value=null;value=current?.artwork?.let{withContext(Dispatchers.IO){ArtworkCache.load(context,it)}}}
    fun toggle(id:String){favorites=if(id in favorites)favorites-id else favorites+id;prefs.edit().putStringSet("favorites",favorites).apply()}
    MaterialTheme(colorScheme=scheme){
        Surface(Modifier.fillMaxSize().onGloballyPositioned{val p=it.positionInWindow();backgroundFrame=Rect(p.x,p.y,p.x+it.size.width,p.y+it.size.height)},color=Ink){
            if(current!=null&&artwork!=null)Image(artwork!!.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=if(backgroundFill)ContentScale.Crop else ContentScale.Fit,alpha=backgroundStrength)
            Box(Modifier.fillMaxSize().then(if(fullTheater)Modifier else Modifier.safeDrawingPadding()),contentAlignment=Alignment.TopCenter){
                Box(Modifier.fillMaxSize().widthIn(max=1100.dp)){
                    val d=catalog.firstOrNull{it.id==selected}
                    // Keep one museum surface for this Activity. Detail/collection screens
                    // suspend it, so returning does not recreate an entire GPU scene.
                    screenState.SaveableStateProvider("theater"){
                        TheaterScreen(catalog,{homeMode="collection"},{fromTheater=true;selected=it},visible=d==null&&homeMode=="theater"&&!about)
                    }
                    if(d==null&&homeMode=="collection")screenState.SaveableStateProvider("collection"){
                        BackHandler{homeMode="theater"}
                        Collection(catalog,favorites,{fromTheater=false;selected=it},{policy="このアプリについて";about=true},{homeMode="theater"},collectionAllPeriods)
                    }
                    else if(d!=null)key(d.id){Exhibit(d,d.id in favorites,{toggle(d.id)},{selected=null},{policy=it;about=true},artwork,backgroundStrength,backgroundFrame,backgroundFill,{artworkInfo=true},fromTheater)}
                }
            }
            if(about){PolicyDialog(policy,{about=false})}
            if(artworkInfo&&current?.artwork!=null)ArtworkDialog(current.artwork,artwork,backgroundStrength,{backgroundStrength=it},backgroundFill,{backgroundFill=it},{artworkInfo=false})
        }
    }
}
@Composable private fun SmallLabel(text:String,modifier:Modifier=Modifier){Text(text,modifier,color=Ochre,fontSize=11.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.3.sp)}
@Composable private fun GlyphButton(icon:ImageVector,label:String,onClick:()->Unit){IconButton(onClick,Modifier.size(48.dp)){Icon(icon,label,tint=Sand)}}
@Composable private fun Collection(catalog:List<Dinosaur>,favorites:Set<String>,open:(String)->Unit,about:()->Unit,theater:()->Unit,allPeriodsState:MutableState<Boolean>){
    var query by rememberSaveable{mutableStateOf("")};var category by rememberSaveable{mutableStateOf("すべて")};var onlyFavorites by rememberSaveable{mutableStateOf(false)}
    val context=LocalContext.current;val maps=remember{PaleoMaps.load(context)}
    val pager=rememberPagerState(initialPage=2,pageCount={GeologicPeriod.entries.size})
    val scope=rememberCoroutineScope()
    var allPeriods by allPeriodsState
    var mapInfo by rememberSaveable{mutableStateOf<Int?>(null)}
    // Restoring a pager can transiently change settledPage during layout. Only an
    // actual map gesture (or the explicit buttons below) clears the saved all-era filter.
    LaunchedEffect(pager){pager.interactionSource.interactions.collect{event->
        if(event is androidx.compose.foundation.interaction.DragInteraction.Start)allPeriods=false
    }}
    val period=GeologicPeriod.entries[pager.settledPage]
    val visible=catalog.filter{(allPeriods||period.contains(it))&&it.matches(query)&&(!onlyFavorites||it.id in favorites)&&when(category){"肉食恐竜"->it.category=="獣脚類"&&it.diet.contains("肉食");"植物食恐竜"->it.diet.contains("植物食");"海と空"->it.category in listOf("海生爬虫類","翼竜類","鳥類に近い恐竜","水辺の爬虫類");else->true}}
    val grid=rememberLazyGridState()
    LazyVerticalGrid(columns=GridCells.Adaptive(160.dp),state=grid,modifier=Modifier.fillMaxSize().testTag("collection-grid"),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=24.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item(key="collection-header",span={GridItemSpan(maxLineSpan)}){Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Outlined.ViewInAr,contentDescription=null,tint=Ochre,modifier=Modifier.size(28.dp));Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)){Text("恐竜の骨格３D",fontWeight=FontWeight.Bold,fontSize=19.sp);SmallLabel("PALEO COLLECTION",Modifier.padding(top=2.dp))}
            GlyphButton(Icons.Outlined.Museum,"シアターへ",theater)
            GlyphButton(Icons.Outlined.Info,"このアプリについて",about)
        }}
        item(key="collection-intro",span={GridItemSpan(maxLineSpan)}){Column(Modifier.padding(top=14.dp,bottom=2.dp)){
            Text("太古の姿を、\nあらゆる角度から。",fontSize=30.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.8).sp)
            Text("${catalog.size}種類の骨格を、手のひらの展示室へ。",Modifier.padding(top=10.dp),fontSize=13.sp,color=Muted)
            Text("Wikipediaの閲覧数を目安に選んだコレクション",Modifier.padding(top=5.dp),fontSize=11.sp,color=Muted)
        }}
        item(key="period-maps",span={GridItemSpan(maxLineSpan)}){
            PeriodCarousel(pager,maps,allPeriods,{page->allPeriods=false;scope.launch{pager.animateScrollToPage(page.coerceIn(0,2))}},{mapInfo=it})
        }
        // Stable item identity retains focus and scroll position across filter changes.
        item(key="collection-search",span={GridItemSpan(maxLineSpan)}){OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("名前・時代・地域で探す",fontSize=14.sp)},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(query.isNotEmpty())GlyphButton(Icons.Outlined.Close,"検索をクリア"){query=""}},singleLine=true,shape=RoundedCornerShape(16.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search))}
        item(key="collection-filters",span={GridItemSpan(maxLineSpan)}){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(allPeriods,{allPeriods=!allPeriods},label={Text("全時代",fontSize=12.sp)})
            listOf("すべて","肉食恐竜","植物食恐竜","海と空").forEach{FilterChip(category==it,{category=it},label={Text(it,fontSize=12.sp)})}
        }}
        item(key="collection-count",span={GridItemSpan(maxLineSpan)}){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text("コレクション",fontWeight=FontWeight.Bold,fontSize=18.sp);Text(if(allPeriods)"全時代" else period.label,Modifier.semantics{contentDescription="表示中の時代：${if(allPeriods)"全時代" else period.label}"},color=Ochre,fontSize=11.sp)};Text("${visible.size} / ${catalog.size}",color=Muted,fontSize=12.sp)
            IconToggleButton(onlyFavorites,{onlyFavorites=it}){Icon(if(onlyFavorites)Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,"お気に入りのみ",tint=if(onlyFavorites)Ochre else Muted)}
        }}
        if(visible.isEmpty())item(key="collection-empty",span={GridItemSpan(maxLineSpan)}){Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.SearchOff,null,tint=Muted);Text("該当する骨格がありません",Modifier.padding(top=12.dp));TextButton({query="";category="すべて";onlyFavorites=false;allPeriods=true}){Text("絞り込みを解除")}}}
        items(visible,key={it.id}){d->Surface(onClick={open(d.id)},shape=corner,color=Panel){Column(Modifier.padding(13.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){SmallLabel(String.format(java.util.Locale.ROOT,"%02d",catalog.indexOf(d)+1));if(d.id in favorites)Icon(Icons.Outlined.Bookmark,null,tint=Ochre,modifier=Modifier.size(15.dp))}
            CollectionIllustration(d,Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(12.dp)))
            Text(d.name,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(d.latin,Modifier.padding(top=3.dp),fontSize=10.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(d.category+" · "+d.period.substringBefore("紀")+"紀",Modifier.padding(top=10.dp),fontSize=10.sp,color=Ochre)
        }}}
        item(key="collection-note",span={GridItemSpan(maxLineSpan)}){Text("骨格は公開3D標本・実測の骨長に基づく復元・模式復元を含みます。モデルごとに使用資料と反映範囲を表示します。翼竜や海生爬虫類など、恐竜とは異なる系統も含みます。",Modifier.padding(top=12.dp),fontSize=11.sp,lineHeight=18.sp,color=Muted)}
        item(key="collection-about",span={GridItemSpan(maxLineSpan)}){TextButton(about,Modifier.fillMaxWidth()){Text("出典・ライセンス・プライバシー",fontSize=12.sp)}}
    }
    mapInfo?.let{index->MapSourcesDialog(GeologicPeriod.entries[index],maps.getValue(GeologicPeriod.entries[index]),{mapInfo=null})}
}
@Composable private fun CollectionIllustration(d:Dinosaur,modifier:Modifier){
    val context=LocalContext.current
    val art by produceState<Bitmap?>(null,d.id){value=d.artwork?.let{withContext(Dispatchers.IO){ArtworkCache.preview(context,it)}}}
    Box(modifier.background(Ink)){
        art?.let{Image(it.asImageBitmap(),"${d.name}の復元イメージ",Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alpha=.6f)}
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha=.12f),Ink.copy(alpha=.25f)))))
        Thumbnail(d.id,Modifier.fillMaxSize().padding(4.dp))
    }
}
private val thumbnailMutex=kotlinx.coroutines.sync.Mutex()
private val thumbnails=android.util.LruCache<String,Bitmap>(12)
@Composable private fun Thumbnail(id:String,modifier:Modifier){
    val assets=LocalContext.current.assets
    val bitmap by produceState<Bitmap?>(thumbnails.get(id),id){if(value==null)value=withContext(Dispatchers.Default){thumbnailMutex.lock();try{makeThumbnail(SkeletonFactory.get(id,assets::open),(ModelProfiles.extras[id]?.base?:id) in listOf("pteranodon","archaeopteryx","mosasaurus"))}finally{thumbnailMutex.unlock()}}.also{thumbnails.put(id,it)}}
    Box(modifier,contentAlignment=Alignment.Center){bitmap?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize())}?:CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=1.dp)}
}
private fun makeThumbnail(mesh:Skeleton,wide:Boolean):Bitmap {
    return specimenThumbnail(mesh,wide)
}
private fun specimenThumbnail(mesh:Skeleton,wide:Boolean=false):Bitmap {
    val bmp=Bitmap.createBitmap(780,360,Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(bmp)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG);val path=Path();val v=mesh.vertices
    val count=mesh.vertexCount;val xy=FloatArray(count*2)
    var loX=Float.MAX_VALUE;var hiX=-Float.MAX_VALUE;var loY=Float.MAX_VALUE;var hiY=-Float.MAX_VALUE
    for(i in 0 until count){val n=i*9;val angle=if(wide).75f else .34f;val x=v[n]*cos(angle)-v[n+2]*sin(angle);val y=v[n+1]+(if(wide).55f else .1f)*(v[n]*sin(angle)+v[n+2]*cos(angle))
        xy[i*2]=x;xy[i*2+1]=y;loX=min(loX,x);hiX=max(hiX,x);loY=min(loY,y);hiY=max(hiY,y)
    }
    val scale=min(710f/(hiX-loX),304f/(hiY-loY));val cx=(loX+hiX)*.5f;val cy=(loY+hiY)*.5f
    for(i in xy.indices step 2){xy[i]=390+(xy[i]-cx)*scale;xy[i+1]=180-(xy[i+1]-cy)*scale}
    (0 until count/3).sortedBy{t->val i=t*27;v[i]*.333f+v[i+2]*.943f}.forEach{t->val i=t*6;val n=t*27
        path.rewind();path.moveTo(xy[i],xy[i+1]);path.lineTo(xy[i+2],xy[i+3]);path.lineTo(xy[i+4],xy[i+5]);path.close()
        val light=(.57f+.35f*max(0f,v[n+4]*.7f+v[n+5]*.7f)).coerceIn(.2f,1f)
        paint.color=mesh.fossilMaterial?.thumbnailColor(v[n],v[n+1],v[n+2],light)?:android.graphics.Color.rgb((220*light).toInt(),(195*light).toInt(),(150*light).toInt());canvas.drawPath(path,paint)
    }
    return bmp
}
private class CameraMemory(var pose:CameraPose=CameraPose(),var view:SkeletonView?=null)
private val cameraSaver=Saver<CameraMemory,List<Float>>(save={val p=it.view?.museumRenderer?.pose?:it.pose;listOf(p.yaw,p.pitch,p.zoom,p.panX,p.panY)},restore={CameraMemory(CameraPose(it[0],it[1],it[2],it[3],it[4]))})

@Composable private fun Exhibit(d:Dinosaur,favorite:Boolean,toggle:()->Unit,back:()->Unit,policy:(String)->Unit,artwork:Bitmap?,backgroundStrength:Float,backgroundFrame:Rect,backgroundFill:Boolean,showArtwork:()->Unit,fromTheater:Boolean=false){
    var tab by rememberSaveable{mutableIntStateOf(if(fromTheater)1 else 0)};var part by rememberSaveable{mutableIntStateOf(0)};var isolate by rememberSaveable{mutableStateOf(false)};var auto by rememberSaveable{mutableStateOf(false)};var fullscreen by rememberSaveable{mutableStateOf(false)}
    val context=LocalContext.current
    var modelInfo by rememberSaveable{mutableStateOf(false)}
    if(modelInfo)AlertDialog(onDismissRequest={modelInfo=false},title={DialogSystemBars();Text("モデルと実寸資料")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(ModelEvidence.label(d.id),fontWeight=FontWeight.Bold)
        Text(ModelEvidence.note(d.id),fontSize=13.sp,lineHeight=22.sp)
        ModelEvidence.source(d.id)?.let{url->TextButton({openLink(context,url)}){Text("標本・計測の出典を開く")}}
    }},confirmButton={TextButton({modelInfo=false}){Text("閉じる")}})
    val compact=LocalConfiguration.current.screenHeightDp < 500
    val defaultPose=if((ModelProfiles.extras[d.id]?.base?:d.id) in listOf("pteranodon","archaeopteryx","mosasaurus"))CameraPose(35f,38f) else CameraPose()
    val memory=rememberSaveable(saver=cameraSaver){CameraMemory(defaultPose)}
    var view by remember{mutableStateOf<SkeletonView?>(null)}
    BackHandler {when{fullscreen->fullscreen=false;tab==1&&!fromTheater->tab=0;else->back()}}
    val mesh by produceState<Skeleton?>(null,d.id){value=withContext(Dispatchers.Default){SkeletonFactory.get(d.id,context.assets::open)}}
    var frameReady by remember(mesh,view){mutableStateOf(false)}
    LaunchedEffect(mesh,view){val expected=mesh?.vertexCount;if(expected!=null){while(view?.museumRenderer?.renderedVertexCount!=expected)kotlinx.coroutines.delay(16);frameReady=true}}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(view,lifecycle){
        val v=view;memory.view=v;val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->v?.onResume();Lifecycle.Event.ON_PAUSE->v?.onPause();else->Unit}}
        lifecycle.addObserver(observer);if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))v?.onResume()
        onDispose{memory.pose=v?.museumRenderer?.pose?:memory.pose;if(memory.view===v)memory.view=null;lifecycle.removeObserver(observer);v?.onPause()}
    }
    val swipeThreshold=with(LocalDensity.current){48.dp.toPx()}
    val showDetails={tab=1;fullscreen=false}
    @Composable fun CameraButtons(){
        Row(horizontalArrangement=Arrangement.Center){
            GlyphButton(if(fullscreen)Icons.Outlined.CloseFullscreen else Icons.Outlined.OpenInFull,if(fullscreen)"通常表示に戻す" else "全画面で観察"){fullscreen=!fullscreen}
            GlyphButton(Icons.Outlined.Add,"拡大"){view?.let{it.camera(it.museumRenderer.pose.scale(1.25f))}}
            GlyphButton(Icons.Outlined.Remove,"縮小"){view?.let{it.camera(it.museumRenderer.pose.scale(.8f))}}
            GlyphButton(Icons.Outlined.RestartAlt,"視点をリセット"){view?.camera(defaultPose);auto=false}
        }
    }
    @Composable fun Evidence(){
        Column(Modifier.padding(horizontal=16.dp,vertical=6.dp)){
            SmallLabel(if(d.id=="triceratops")"公開標本  /  前後幅 約5.89 m" else "${d.category}  /  属の目安：${d.size}")
            Text(ModelEvidence.label(d.id),Modifier.padding(top=4.dp),fontSize=11.sp,color=Muted)
            d.artwork?.note?.takeIf{it.isNotBlank()}?.let{Text(it,fontSize=10.sp,color=Ochre)}
        }
    }
    @Composable fun ModelCanvas(modifier:Modifier){
        // Layout gives GL its entire visible rectangle; no header or operation card covers the bones.
        Box(modifier.testTag("skeleton-viewport")){
            AndroidView(factory={ctx->SkeletonView(ctx).also{val saved=memory.view?.museumRenderer?.pose?:memory.pose;view=it;memory.view=it;it.camera(saved)}},modifier=Modifier.fillMaxSize(),update={v->mesh?.let{if(v.tag!==it){v.setModel(it);v.tag=it}};v.background(artwork,backgroundStrength,floatArrayOf(backgroundFrame.left,backgroundFrame.top,backgroundFrame.width,backgroundFrame.height),backgroundFill);v.highlight(part,isolate);v.rotate(auto)})
            if(!frameReady)CircularProgressIndicator(Modifier.align(Alignment.Center))
            mesh?.metresPerUnit?.let{unit->Text("床の格子：${gridMetres(unit)} m",Modifier.align(Alignment.BottomEnd).padding(8.dp),fontSize=10.sp,color=Muted)}
        }
    }
    @Composable fun ObservationCard(modifier:Modifier){
        Column(modifier.clip(RoundedCornerShape(18.dp)).background(Panel.copy(alpha=.94f))
            .semantics{contentDescription="部位と観察のカード";customActions=listOf(CustomAccessibilityAction("詳細図鑑を開く"){showDetails();true})}
            .pointerInput(swipeThreshold){var distance=0f;detectVerticalDragGestures(onDragStart={distance=0f},onDragCancel={distance=0f},onDragEnd={if(distance < -swipeThreshold)showDetails();distance=0f}){change,dy->distance+=dy;change.consume()}}
            .padding(horizontal=12.dp,vertical=4.dp)){
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top=3.dp).size(32.dp,4.dp).clip(RoundedCornerShape(2.dp)).background(Muted.copy(alpha=.5f)))
            TextButton(showDetails,Modifier.align(Alignment.CenterHorizontally).height(40.dp),contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)){
                Icon(Icons.Outlined.KeyboardArrowUp,null,Modifier.size(18.dp));Text("上にスワイプして詳細図鑑",fontSize=11.sp)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
                listOf("側面" to CameraPose(0f,0f),"正面" to CameraPose(90f,0f),"背面" to CameraPose(-90f,0f),"上面" to CameraPose(0f,89f),"下面" to CameraPose(0f,-89f)).forEach{(name,c)->OutlinedButton({view?.camera(c);auto=false},contentPadding=PaddingValues(horizontal=13.dp,vertical=0.dp)){Text(name,fontSize=12.sp)}}
                FilterChip(auto,{auto=!auto},label={Text("自動回転",fontSize=12.sp)})
            }
            var expanded by remember{mutableStateOf(false)}
            Row(verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.weight(1f)){TextButton({expanded=true},contentPadding=PaddingValues(horizontal=4.dp)){Icon(Icons.Outlined.Layers,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("部位：${BoneGroups.names[part]}",fontSize=12.sp);Icon(Icons.Outlined.ExpandMore,null)}
                    DropdownMenu(expanded,{expanded=false}){BoneGroups.names.forEachIndexed{i,s->if(i==0 || mesh?.groups?.contains(i)==true)DropdownMenuItem(text={Text(s)},onClick={part=i;expanded=false})}}}
                Switch(isolate,{isolate=it},modifier=Modifier.semantics{contentDescription="選択部位のみを表示"},enabled=part!=0)
            }
            if(!compact){
                Text("選択部位のみ：右のスイッチで切り替え",fontSize=10.sp,color=Muted)
                Text(if(d.id in SpecimenLoader.ids)"座標による簡易的な領域を選択します。境界には隣接する骨の一部が含まれます。"+(if(d.id=="triceratops")"角と襟飾りは頭骨に含みます。" else "") else BoneGroups.descriptions[part],fontSize=11.sp,lineHeight=17.sp,color=Muted,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
        }
    }
    @Composable fun Sources(){
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.SpaceBetween){
            TextButton({modelInfo=true}){Text("モデルと実寸資料",fontSize=11.sp)}
            if(d.artwork!=null)TextButton(showArtwork){Icon(Icons.Outlined.Image,null,Modifier.size(16.dp));Text("背景と出典",fontSize=11.sp)}
        }
    }
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().background(Ink.copy(alpha=.75f)).padding(horizontal=8.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically){
            GlyphButton(Icons.AutoMirrored.Outlined.ArrowBack,if(fromTheater)"展示に戻る" else "一覧に戻る",back)
            Column(Modifier.weight(1f)){Text(d.name,fontSize=19.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(d.latin,color=Muted,fontSize=11.sp)}
            if(compact&&!fullscreen)listOf("3D骨格","詳細図鑑").forEachIndexed{i,name->FilterChip(tab==i,{tab=i},label={Text(name)},modifier=Modifier.padding(horizontal=4.dp))}
            GlyphButton(if(favorite)Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,if(favorite)"お気に入りから削除" else "お気に入りに追加",toggle)
        }
        if(!fullscreen&&!compact)Row(Modifier.fillMaxWidth().background(Ink.copy(alpha=.65f)).padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf("3D骨格","詳細図鑑").forEachIndexed{index,s->FilterChip(tab==index,{tab=index},label={Text(s)},leadingIcon={Icon(if(index==0)Icons.Outlined.ViewInAr else Icons.AutoMirrored.Outlined.MenuBook,null,Modifier.size(17.dp))})}
        }
        if(tab==1&&!fullscreen){Details(d,policy){tab=0};return@Column}
        if(fullscreen){
            ModelCanvas(Modifier.fillMaxWidth().weight(1f))
            Row(Modifier.fillMaxWidth().background(Panel),horizontalArrangement=Arrangement.Center){CameraButtons();TextButton(showDetails){Text("詳細図鑑")}}
        }else if(compact){
            Row(Modifier.fillMaxWidth().weight(1f)){
                ModelCanvas(Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.width(226.dp).fillMaxHeight().background(Ink.copy(alpha=.8f)).verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally){
                    CameraButtons();ObservationCard(Modifier.fillMaxWidth());Evidence();Sources()
                    TextButton({policy("復元モデルについて")}){Text("復元の限界と出典",fontSize=11.sp)}
                }
            }
        }else{
            Evidence()
            ModelCanvas(Modifier.fillMaxWidth().weight(1f))
            Column(Modifier.fillMaxWidth().background(Ink.copy(alpha=.6f)),horizontalAlignment=Alignment.CenterHorizontally){
                CameraButtons();Text("1本指で回転 · 2本指で拡大・移動",fontSize=10.sp,color=Muted);Sources()
            }
            ObservationCard(Modifier.fillMaxWidth())
            TextButton({policy("復元モデルについて")},Modifier.height(32.dp),contentPadding=PaddingValues(horizontal=16.dp,vertical=0.dp)){Text("復元の限界と出典",fontSize=11.sp)}
        }
    }
}
@Composable private fun Details(d:Dinosaur,policy:(String)->Unit,close:()->Unit){
    val context=LocalContext.current
    val listState=rememberLazyListState()
    val threshold=with(LocalDensity.current){48.dp.toPx()}
    val onClose by rememberUpdatedState(close)
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha=.88f))){
        Surface(color=Panel,modifier=Modifier.fillMaxWidth()
            .semantics{contentDescription="詳細図鑑を閉じるハンドル";customActions=listOf(CustomAccessibilityAction("骨格に戻る"){onClose();true})}
            .pointerInput(threshold){var distance=0f;detectVerticalDragGestures(onDragStart={distance=0f},onDragCancel={distance=0f},onDragEnd={if(distance>threshold)onClose();distance=0f}){change,dy->distance+=dy;change.consume()}}){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Box(Modifier.padding(top=8.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Muted))
                TextButton({onClose()},contentPadding=PaddingValues(horizontal=12.dp,vertical=0.dp)){
                    Icon(Icons.Outlined.KeyboardArrowDown,null,Modifier.size(18.dp));Text("下にスワイプして骨格に戻る",fontSize=12.sp)
                }
            }
        }
    LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("details-list")
        .pointerInput(d.id,threshold){
            awaitEachGesture {
                val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
                // A downward reading scroll that reaches the top must not close
                // the page. Eligibility is fixed at the beginning of the gesture.
                var eligible=!listState.canScrollBackward
                var distance=0f
                do {
                    val event=awaitPointerEvent(PointerEventPass.Initial)
                    val pointer=event.changes.firstOrNull{it.id==down.id}?:break
                    val delta=pointer.position-down.position
                    if(event.changes.size!=1||delta.y < -viewConfiguration.touchSlop||abs(delta.x)>max(viewConfiguration.touchSlop,abs(delta.y)))eligible=false
                    distance=delta.y
                    if(eligible&&distance>threshold)pointer.consume()
                    val pressed=pointer.pressed
                } while(pressed)
                if(eligible&&distance>threshold)onClose()
            }
        },state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{SmallLabel("FIELD NOTES  /  ${d.category}");Text(d.subtitle,Modifier.padding(top=10.dp),fontSize=24.sp,fontWeight=FontWeight.Bold,lineHeight=34.sp);Text(d.summary,Modifier.padding(top=12.dp),fontSize=14.sp,lineHeight=25.sp,color=Muted)}
        item{Surface(shape=corner,color=Panel){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
            Fact("生きていた時代",d.period+"\n"+d.age);Fact("化石の主な産地",d.region);Fact("大きさの目安",d.size);Fact("食性",d.diet);Fact("閲覧数による選定", "収録候補中 ${d.rank}位 · ${String.format(java.util.Locale.JAPAN,"%,d",d.pageviews)}回\n英語版Wikipedia · 2026年6〜8月")
        }}}
        items(d.sections.size){i->val s=d.sections[i];Column{Row(verticalAlignment=Alignment.CenterVertically){SmallLabel(String.format(java.util.Locale.ROOT,"%02d",i+1));Spacer(Modifier.width(10.dp));Text(s.title,fontSize=17.sp,fontWeight=FontWeight.SemiBold)};Text(s.text,Modifier.padding(top=9.dp),fontSize=14.sp,lineHeight=25.sp,color=Muted)
            s.references.forEach{ref->TextButton({openLink(context,ref.url)},contentPadding=PaddingValues(vertical=4.dp,horizontal=0.dp)){Text("出典：${ref.title}",fontSize=11.sp,modifier=Modifier.weight(1f));Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(14.dp))}}
        }}
        item{Surface(shape=corner,color=Panel){Column(Modifier.padding(18.dp)){SmallLabel("この骨格モデルについて");Text(ModelEvidence.note(d.id),Modifier.padding(top=10.dp),fontSize=13.sp,lineHeight=22.sp);ModelEvidence.source(d.id)?.let{url->TextButton({openLink(context,url)}){Text("標本・計測の出典を開く")}}}}}
        item{Text("さらに詳しく",fontSize=19.sp,fontWeight=FontWeight.Bold);Text("外部アプリ・ブラウザーで開きます。閲覧には通信が必要です。",Modifier.padding(top=6.dp),fontSize=12.sp,color=Muted)
            OutlinedButton({openLink(context,d.wikipedia)},Modifier.fillMaxWidth().padding(top=10.dp)){Icon(Icons.AutoMirrored.Outlined.MenuBook,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("Wikipediaで詳しく読む")}
            OutlinedButton({openLink(context,d.youtube)},Modifier.fillMaxWidth()){Icon(Icons.Outlined.PlayCircleOutline,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("YouTubeの関連動画を探す")}
            Text("YouTubeの検索結果です。特定の動画の内容・権利・正確性を保証するものではありません。",fontSize=11.sp,lineHeight=17.sp,color=Muted)}
        if(d.videos.isNotEmpty()) {
            item{Text("関連動画",fontSize=19.sp,fontWeight=FontWeight.Bold);Text("公開元で視聴できます。制作当時の復元・学説を含み、現在の図鑑解説と異なる場合があります。",Modifier.padding(top=6.dp),fontSize=12.sp,lineHeight=20.sp,color=Muted)}
            items(d.videos.size){i->val video=d.videos[i];OutlinedButton({openLink(context,video.url)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(14.dp)){Icon(Icons.Outlined.PlayCircleOutline,null,Modifier.size(20.dp));Spacer(Modifier.width(10.dp));Text(video.title,Modifier.weight(1f),fontSize=13.sp)}}
        }
        item{HorizontalDivider(color=Border);Text("出典・解説のライセンス",Modifier.padding(top=18.dp),fontSize=18.sp,fontWeight=FontWeight.Bold)
            Text("確認日 ${Catalog.REVIEW_DATE}\n名前の由来・生態・特徴・発見史・未解明な点を増補し、参照した研究は各節にリンクしています。新説は報告年と限界を示し、すべての項目に同じ年の新研究があるという意味ではありません。\n\n解説にはWikipedia contributorsによる日本語版の抄録、英語版の要約・再構成を含みます。引用番号・画像・表・一部の節を省略し、補足と更新注記を加えました。解説全体をCC BY-SA 4.0で提供します。研究論文・博物館資料の本文や図版を転載するものではありません。以下から原文・編集履歴を確認できます。",Modifier.padding(top=10.dp),fontSize=12.sp,lineHeight=21.sp,color=Muted)}
        items(d.sources.size){i->val source=d.sources[i];OutlinedButton({openLink(context,source.url)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(14.dp)){Text(source.title,Modifier.weight(1f),fontSize=12.sp);Icon(Icons.AutoMirrored.Outlined.OpenInNew,null,Modifier.size(15.dp))}}
        item{TextButton({openLink(context,"https://creativecommons.org/licenses/by-sa/4.0/deed.ja")}){Text("CC BY-SA 4.0 の条件")}
            Text("解説は確認日時点の主要情報を整理したものです。新しい研究を自動反映するものではなく、既知のすべての情報を網羅したものでもありません。推定値と復元には幅があります。",fontSize=12.sp,lineHeight=20.sp,color=Muted)}
        item{TextButton({policy("プライバシーポリシー")},Modifier.fillMaxWidth()){Text("プライバシーポリシー")}}
    }
    }
}
@Composable private fun Fact(label:String,value:String){Row(Modifier.fillMaxWidth()){Text(label,Modifier.width(115.dp),fontSize=12.sp,color=Muted);Text(value,fontSize=13.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f))}}
@Composable private fun ArtworkDialog(art:Artwork,bitmap:Bitmap?,strength:Float,change:(Float)->Unit,fill:Boolean,changeFill:(Boolean)->Unit,close:()->Unit){
    val context=LocalContext.current
    AlertDialog(onDismissRequest=close,title={DialogSystemBars();Text("背景と出典")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        bitmap?.let{Image(it.asImageBitmap(),"生体の復元イメージ",Modifier.fillMaxWidth().height(150.dp),contentScale=ContentScale.Fit)}
        Text("背景の濃さ ${ (strength*100).toInt() }%",fontSize=13.sp)
        Slider(strength,change,modifier=Modifier.semantics{contentDescription="背景の濃さ"})
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(fill,{changeFill(true)},label={Text("画面に広げる",fontSize=11.sp)});FilterChip(!fill,{changeFill(false)},label={Text("画像全体",fontSize=11.sp)})}
        Text("生体復元画です。色や軟組織、姿勢には推定が含まれ、骨格モデルと細部が異なる場合があります。",fontSize=12.sp)
        if(art.note.isNotBlank())Text(art.note,fontSize=12.sp,color=Ochre)
        Text(art.title,fontSize=12.sp);Text("作者：${art.artist}",fontSize=12.sp)
        if(art.credit.isNotBlank())Text(art.credit,fontSize=11.sp)
        Text("${art.license} · 画面への配置時にトリミング・明度調整",fontSize=12.sp)
        TextButton({openLink(context,art.source)}){Text("画像の原本・権利情報")}
        TextButton({openLink(context,art.licenseUrl)}){Text("画像のライセンス条件")}
    }},confirmButton={TextButton(close){Text("閉じる")}})
}
internal fun openLink(context:Context,url:String){
    val uri=Uri.parse(url)
    if(uri.scheme!="https"||uri.host.isNullOrBlank())return
    try{context.startActivity(Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE))}catch(_:ActivityNotFoundException){Toast.makeText(context,"リンクを開くアプリがありません。ブラウザーをインストールしてください。",Toast.LENGTH_LONG).show()}
}
@Composable internal fun PolicyDialog(initial:String,close:()->Unit){
    var page by rememberSaveable{mutableStateOf(initial)};val context=LocalContext.current
    val content=remember(page){context.assets.open(when(page){"館内の縮尺"->"theater/scale-notes.txt";"プライバシーポリシー"->"privacy.txt";"ライセンス"->"licenses.txt";"復元モデルについて"->"models.txt";"収録種の選び方"->"popularity.txt";else->"about.txt"}).bufferedReader().use{it.readText()}}
    AlertDialog(onDismissRequest=close,title={DialogSystemBars();Text(page)},text={Column(Modifier.verticalScroll(rememberScrollState())){
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("このアプリについて","館内の縮尺","復元モデルについて","収録種の選び方","ライセンス","プライバシーポリシー").forEach{FilterChip(page==it,{page=it},label={Text(it,fontSize=11.sp)})}}
        Text(content,fontSize=13.sp,lineHeight=22.sp)
        if(page=="ライセンス"){TextButton({openLink(context,"https://creativecommons.org/licenses/by-sa/4.0/deed.ja")}){Text("CC BY-SA 4.0 の条件")}}
        Text("Version ${BuildConfig.VERSION_NAME}",Modifier.padding(top=16.dp),fontSize=11.sp,color=Muted)
    }},confirmButton={TextButton(close){Text("閉じる")}})
}
