package io.github.hatake716.dinobones.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.hatake716.dinobones.data.Dinosaur
import io.github.hatake716.dinobones.theater.*

private class WalkMemory(var pose:WalkPose=WalkPose(),var view:TheaterView?=null)
private val walkSaver=Saver<WalkMemory,List<Float>>(save={val p=it.view?.renderer?.pose?:it.pose;listOf(p.x,p.z,p.yaw,p.pitch)},restore={WalkMemory(TheaterWorld.safe(WalkPose(it[0],it[1],it[2],it[3])))})

@Composable fun TheaterScreen(catalog:List<Dinosaur>,collection:()->Unit,open:(String)->Unit,visible:Boolean=true){
    val memory=rememberSaveable(saver=walkSaver){WalkMemory()}
    var view by remember{mutableStateOf<TheaterView?>(null)}
    var current by remember{mutableStateOf(memory.pose)}
    var target by remember{mutableStateOf<ExhibitHit?>(null)}
    var ready by remember{mutableStateOf(false)}
    var failed by remember{mutableStateOf<String?>(null)}
    var map by rememberSaveable{mutableStateOf(false)}
    var credits by rememberSaveable{mutableStateOf<String?>(null)}
    var illustration by rememberSaveable{mutableStateOf<String?>(null)}
    var help by rememberSaveable{mutableStateOf(false)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val compact=LocalConfiguration.current.screenHeightDp<500
    val titleScrim=remember{Brush.verticalGradient(listOf(Ink.copy(alpha=.97f),Ink.copy(alpha=.82f),Color.Transparent))}
    val animals=remember(catalog){catalog.associateBy{it.id}}
    val screenVisible by rememberUpdatedState(visible)
    val activeView=view
    DisposableEffect(activeView,lifecycle){
        val active=activeView
        val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->if(screenVisible&&!map&&!help&&credits==null&&illustration==null)active?.resume();Lifecycle.Event.ON_PAUSE->active?.pause();else->Unit}}
        lifecycle.addObserver(observer)
        onDispose{lifecycle.removeObserver(observer);active?.let{memory.pose=it.renderer.pose;memory.view=null;it.release()}}
    }
    // Dialogs should stop walking and rendering; they do not obscure a still-running simulation.
    LaunchedEffect(visible,map,help,credits,illustration,view){view?.let{if(!visible||map||help||credits!=null||illustration!=null)it.pause() else if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))it.resume()}}
    Box(Modifier.fillMaxSize().then(if(visible)Modifier else Modifier.alpha(0f).clearAndSetSemantics{})){
        AndroidView(factory={ctx->TheaterView(ctx).also{v->
            v.renderer.pose=memory.pose;v.onPick=open;v.onArtwork={illustration=it};v.onError={failed=it}
            v.renderer.onUpdate={p,id,count->current=p;target=id?.takeIf{it.artwork||it.spot.id in v.renderer.renderedIds};if(count>0)ready=true}
            memory.view=v;view=v
        }},modifier=Modifier.fillMaxSize(),update={v->v.visibility=if(visible)android.view.View.VISIBLE else android.view.View.INVISIBLE;v.importantForAccessibility=if(visible)android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO else android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS})
        if(visible&&compact)Row(Modifier.fillMaxWidth().displayCutoutPadding().padding(horizontal=12.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.widthIn(max=240.dp).background(titleScrim,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=7.dp)){
                Text("恐竜の骨格３D",color=Sand,fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1)
                Text("0${TheaterWorld.section(current)+1} · ${TheaterWorld.sectionNames[TheaterWorld.section(current)]}",color=Muted,fontSize=10.sp,maxLines=1)
            }
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton({help=true}){Icon(Icons.AutoMirrored.Outlined.HelpOutline,"館内の歩き方",Modifier.size(22.dp))}
            FilledTonalButton({map=true},contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp)){
                Icon(Icons.Outlined.Map,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("館内マップ",fontSize=12.sp)
            }
            FilledTonalButton(collection,contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp)){
                Icon(Icons.AutoMirrored.Outlined.MenuBook,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("図鑑一覧",fontSize=12.sp)
            }
        }
        if(visible&&!compact)Column(Modifier.fillMaxWidth().background(titleScrim).padding(horizontal=16.dp,vertical=10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("PALEO MUSEUM",color=Ochre,fontSize=10.sp,letterSpacing=2.4.sp,fontWeight=FontWeight.Bold)
                    Text("恐竜の骨格３D",color=Sand,fontSize=if(compact)18.sp else 22.sp,fontWeight=FontWeight.Bold)
                }
                IconButton({help=true}){Icon(Icons.AutoMirrored.Outlined.HelpOutline,"館内の歩き方",tint=Sand)}
                FilledTonalButton(collection,contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp)){
                    Icon(Icons.AutoMirrored.Outlined.MenuBook,null,Modifier.size(17.dp));Spacer(Modifier.width(5.dp));Text("図鑑一覧",fontSize=12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("0${TheaterWorld.section(current)+1}  /  ${TheaterWorld.sectionNames[TheaterWorld.section(current)]}",Modifier.weight(1f),color=Muted,fontSize=11.sp,maxLines=1)
                TextButton({map=true},contentPadding=PaddingValues(horizontal=8.dp)){
                    Icon(Icons.Outlined.Map,null,Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("館内マップ",fontSize=12.sp)
                }
            }
            if(!compact)Text("100の骨格が待つ、太古への散歩。",color=Sand.copy(alpha=.8f),fontSize=11.sp)
        }
        if(visible&&!ready&&failed==null)Surface(Modifier.align(Alignment.Center).padding(24.dp),color=Panel.copy(alpha=.94f),shape=RoundedCornerShape(16.dp)){
            Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp);Spacer(Modifier.width(12.dp));Text("展示室を準備しています",fontSize=13.sp)}
        }
        if(visible)Column(Modifier.align(Alignment.BottomEnd).then(if(compact)Modifier.displayCutoutPadding() else Modifier).widthIn(max=220.dp).padding(end=16.dp,bottom=18.dp),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(6.dp)){
            val d=animals[target?.spot?.id]
            if(d!=null)FilledTonalButton({if(target?.artwork==true)illustration=d.id else open(d.id)},shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(12.dp)){
                Column{Text(d.name,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=13.sp);Text(ExhibitScales.entries.getValue(d.id).label,fontSize=10.sp);Text(if(target?.artwork==true)"復元イラストを開く" else "詳細図鑑を開く",fontSize=11.sp)}
                Spacer(Modifier.width(8.dp));Icon(Icons.Outlined.TouchApp,null,Modifier.size(18.dp))
            }
            if(failed!=null)Surface(color=Panel,shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(12.dp)){
                Text("一部の展示を読み込めませんでした",fontSize=11.sp);TextButton({view?.retry()}){Text("再読み込み")}
            }}
            Text("ドラッグで見回す · 骨格や絵をタップ",Modifier.background(Ink.copy(alpha=.75f),RoundedCornerShape(6.dp)).padding(6.dp),color=Sand,fontSize=10.sp)
            TextButton({view?.travel(null);current=WalkPose()}){Icon(Icons.Outlined.MeetingRoom,null,Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("入口へ",fontSize=11.sp)}
        }
    }
    illustration?.let{ id->animals[id]?.let{d->RestorationViewer(d){illustration=null}}}
    if(map)MuseumMap(catalog,current,{map=false}){spot->view?.travel(spot);current=view?.renderer?.pose?:spot.arrival();target=null;map=false}
    credits?.let{page->PolicyDialog(page){credits=null}}
    if(help)AlertDialog(onDismissRequest={help=false},title={DialogSystemBars();Text("館内の歩き方")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("左下のスティックで前後・左右に移動。深く倒すと速く、浅く倒すとゆっくり進みます。画面をドラッグすると視点が動きます。二本の指で、歩きながら見回すこともできます。")
        Text("骨格または展示台の案内板をタップすると詳細図鑑を開きます。隣の「復元イラスト」の立て札をタップすると生体復元画を拡大して閲覧できます。二本指で拡大・移動でき、「作者・出典」から画像の権利情報を確認できます。中央の照準で選び、名前のボタンから開くこともできます。戻ると同じ場所から散策を再開します。")
        Text("5つの展示室に全100種類を展示しています。館内マップから好きな骨格の前へ移動できます。「図鑑一覧」には従来の時代別メニューがあります。")
        Text("ロイヤル・ティレル古生物学博物館の大骨格展示を参考に、天窓からの採光・シダと木生シダの植栽・大きな展示台で構成した独自の仮想博物館です。実在館の建物や展示配置の正確な再現ではなく、公式・提携アプリでもありません。",fontSize=12.sp)
        Text("視点の高さは1.7 m、床の目地は1 m。実寸資料のある骨格は元の単位を保持し、他は推定全長・翼開長に換算しています。縮尺未確定の種は参考展示です。小型標本は拡大せず観察台に載せています。頭尾方向の幅を合わせた近似なので、姿勢や未計測部分の正確さを保証しません。",fontSize=12.sp)
        TextButton({help=false;credits="館内の縮尺"}){Text("寸法の根拠と出典を確認")}
    }},dismissButton={TextButton({help=false;credits="ライセンス"}){Text("出典・ライセンス")}},confirmButton={TextButton({help=false}){Text("散策を続ける")}})
}

@Composable private fun MuseumMap(catalog:List<Dinosaur>,pose:WalkPose,close:()->Unit,travel:(ExhibitSpot)->Unit){
    val compact=LocalConfiguration.current.screenHeightDp<500
    var query by rememberSaveable{mutableStateOf("")}
    var section by rememberSaveable{mutableIntStateOf(-1)}
    val animals=remember(catalog){catalog.associateBy{it.id}}
    val visible=TheaterWorld.spots.filter{(section<0||it.section==section)&&animals.getValue(it.id).matches(query)}
    BackHandler(onBack=close)
    Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
        DialogSystemBars()
        val keyboard=LocalSoftwareKeyboardController.current
        val focusManager=LocalFocusManager.current
        Surface(Modifier.fillMaxWidth().widthIn(max=760.dp).fillMaxHeight(.94f).padding(horizontal=12.dp),shape=RoundedCornerShape(24.dp),color=Panel){
            Column(Modifier.padding(18.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("館内マップ",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("展示を選ぶと、骨格の前へ移動します",color=Muted,fontSize=11.sp)};IconButton(close){Icon(Icons.Outlined.Close,"館内マップを閉じる")}}
                if(compact)Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    for(s in 0..4)FilterChip(section==s,{section=if(section==s)-1 else s},label={Text("展示室 ${s+1}",fontSize=11.sp)})
                }
                else Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    for(s in 0..4)Column(Modifier.weight(1f).background(if(section==s)Border else Ink,RoundedCornerShape(9.dp)).clickable{section=if(section==s)-1 else s}.padding(7.dp).semantics{contentDescription="展示室${s+1}、20種類"},horizontalAlignment=Alignment.CenterHorizontally){
                        Text("0${s+1}",fontSize=12.sp,color=Ochre)
                        Canvas(Modifier.fillMaxWidth().height(62.dp)){
                            for(row in 0..4)for(col in 0..3)drawCircle(Muted.copy(alpha=.7f),2.7.dp.toPx(),Offset(size.width*(col+.5f)/4,size.height*(row+.5f)/5))
                            if(TheaterWorld.section(pose)==s)drawCircle(Ochre,4.dp.toPx(),Offset(size.width*((pose.x+TheaterWorld.halfWidth)/(TheaterWorld.halfWidth*2)).coerceIn(.03f,.97f),size.height*TheaterWorld.mapDepth(pose)))
                        }
                        Text(if(TheaterWorld.section(pose)==s)"現在地" else "20種",color=Muted,fontSize=10.sp)
                    }
                }
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={focusManager.clearFocus();keyboard?.hide()}),placeholder={Text("展示の名前・地域で探す",fontSize=13.sp)},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(query.isNotEmpty())IconButton({query=""}){Icon(Icons.Outlined.Close,"展示検索をクリア")}})
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("${visible.size} / 100 展示",Modifier.weight(1f),color=Muted,fontSize=12.sp);TextButton({section=-1;query=""}){Text("すべて表示",fontSize=12.sp)}}
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(visible,key={it.id}){spot->val d=animals.getValue(spot.id)
                        Surface(onClick={travel(spot)},shape=RoundedCornerShape(12.dp),color=Ink,modifier=Modifier.fillMaxWidth().semantics{contentDescription="${d.name}の展示へ移動"}){
                            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(String.format(java.util.Locale.ROOT,"%02d",spot.index+1),color=Ochre,fontSize=13.sp,modifier=Modifier.width(32.dp));Column(Modifier.weight(1f)){
                                Text(d.name,fontSize=14.sp,fontWeight=FontWeight.Bold);Text(d.latin,fontSize=10.sp,color=Muted);Text("0${spot.section+1} · ${TheaterWorld.sectionNames[spot.section]}",fontSize=10.sp,color=Ochre)
                            };Icon(Icons.Outlined.NearMe,"展示へ移動",Modifier.size(20.dp),tint=Ochre)}
                        }
                    }
                    if(visible.isEmpty())item{Text("該当する展示がありません",Modifier.padding(vertical=24.dp),color=Muted)}
                }
            }
        }
    }
}
