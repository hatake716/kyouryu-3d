package io.github.hatake716.dinobones.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.hatake716.dinobones.data.ArtworkCache
import io.github.hatake716.dinobones.data.Dinosaur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only the selected original is decoded; all museum signs share a thumbnail atlas. */
@Composable internal fun RestorationViewer(d:Dinosaur,close:()->Unit){
    val art=d.artwork?:return
    val context=LocalContext.current
    var failed by remember(d.id){mutableStateOf(false)}
    val bitmap by produceState<Bitmap?>(null,d.id){
        value=withContext(Dispatchers.IO){runCatching{ArtworkCache.load(context,art)}.getOrNull()}
        failed=value==null
    }
    var scale by rememberSaveable(d.id){mutableFloatStateOf(1f)}
    var panX by rememberSaveable(d.id){mutableFloatStateOf(0f)}
    var panY by rememberSaveable(d.id){mutableFloatStateOf(0f)}
    var source by rememberSaveable(d.id){mutableStateOf(false)}
    var size by remember{mutableStateOf(IntSize.Zero)}
    fun clampPan(){
        val image=bitmap?:return
        val fit=minOf(size.width.toFloat()/image.width,size.height.toFloat()/image.height)
        val maxX=((image.width*fit*scale-size.width)/2).coerceAtLeast(0f)
        val maxY=((image.height*fit*scale-size.height)/2).coerceAtLeast(0f)
        panX=panX.coerceIn(-maxX,maxX);panY=panY.coerceIn(-maxY,maxY)
    }
    LaunchedEffect(size,bitmap){clampPan()}
    val transform=rememberTransformableState{zoom,pan,_->
        scale=(scale*zoom).coerceIn(1f,6f);panX+=pan.x;panY+=pan.y;clampPan()
    }
    Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
        DialogSystemBars()
        Surface(Modifier.fillMaxSize(),color=Ink){
            Column(Modifier.safeDrawingPadding()){
                Row(Modifier.fillMaxWidth().padding(start=16.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("${d.name}の復元イラスト",Modifier.weight(1f),fontSize=17.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    IconButton({scale=1f;panX=0f;panY=0f}){Icon(Icons.Outlined.RestartAlt,"イラストの表示をリセット")}
                    IconButton(close){Icon(Icons.Outlined.Close,"復元イラストを閉じる")}
                }
                Box(Modifier.fillMaxWidth().weight(1f).background(Panel).clipToBounds().onSizeChanged{size=it}
                    .testTag("restoration-image").semantics{stateDescription="拡大率 ${(scale*100).toInt()}%"}
                    .transformable(transform),contentAlignment=Alignment.Center){
                    bitmap?.let{Image(it.asImageBitmap(),"${d.name}の生体復元画",Modifier.fillMaxSize().graphicsLayer{scaleX=scale;scaleY=scale;translationX=panX;translationY=panY},contentScale=ContentScale.Fit)}
                        ?:if(failed)Text("画像を読み込めませんでした") else CircularProgressIndicator()
                }
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("2本指で拡大・移動",Modifier.weight(1f),color=Muted,fontSize=11.sp)
                    TextButton({source=true}){Text("作者・出典")}
                }
            }
        }
    }
    if(source)AlertDialog(onDismissRequest={source=false},title={DialogSystemBars();Text("復元イラストの作者・出典")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text(art.title,fontSize=14.sp);Text("作者：${art.artist}",fontSize=13.sp)
            if(art.credit.isNotBlank())Text(art.credit,fontSize=12.sp)
            Text(art.license,fontSize=13.sp)
            Text("生体復元画です。色・軟組織・姿勢には推定が含まれ、骨格モデルと細部が異なる場合があります。",fontSize=12.sp)
            if(art.note.isNotBlank())Text(art.note,fontSize=12.sp,color=Ochre)
            Text("立て札では余白と見出しを付けて縮小表示。閲覧画面では画像全体を表示し、拡大・移動できます。",fontSize=12.sp)
            TextButton({openLink(context,art.source)}){Text("画像の原本・権利情報")}
            TextButton({openLink(context,art.licenseUrl)}){Text("画像のライセンス条件")}
        }
    },confirmButton={TextButton({source=false}){Text("イラストに戻る")}})
}
