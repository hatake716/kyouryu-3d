package io.github.hatake716.dinobones.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hatake716.dinobones.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable internal fun PeriodCarousel(pager:PagerState,maps:Map<GeologicPeriod,Artwork>,allPeriods:Boolean,choose:(Int)->Unit,info:(Int)->Unit) {
    val context=LocalContext.current
    Surface(shape=RoundedCornerShape(20.dp),color=Panel){
        Column(Modifier.padding(vertical=12.dp)){
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){
                Text("時代の地図",Modifier.weight(1f),fontSize=13.sp,color=Ochre,fontWeight=FontWeight.Bold)
                Text("${pager.settledPage+1} / 3",fontSize=11.sp,color=Muted)
            }
            HorizontalPager(pager,Modifier.fillMaxWidth().semantics{contentDescription="時代の世界地図"},key={GeologicPeriod.entries[it].name}) { page ->
                val period=GeologicPeriod.entries[page];val art=maps.getValue(period)
                val bitmap by produceState<Bitmap?>(null,art.asset){value=withContext(Dispatchers.IO){ArtworkCache.load(context,art)}}
                Column(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text(period.label,fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        Text("地図：${period.mapAge}",fontSize=11.sp,color=Muted)
                    }
                    Text(period.range,fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=3.dp,bottom=10.dp))
                    Box(Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(10.dp)).background(Ink),contentAlignment=Alignment.Center){
                        bitmap?.let{Image(it.asImageBitmap(),"${period.label}の世界地図（${period.mapAge}）",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}
                            ?:CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){
                IconButton({choose(pager.settledPage-1)},enabled=pager.settledPage>0){Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,"以前の時代")}
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(5.dp,Alignment.CenterHorizontally)){
                    GeologicPeriod.entries.forEachIndexed{index,era->
                        FilterChip(!allPeriods&&pager.settledPage==index,{choose(index)},label={Text(era.label,fontSize=11.sp)},modifier=Modifier.height(36.dp))
                    }
                }
                IconButton({choose(pager.settledPage+1)},enabled=pager.settledPage<2){Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"次の時代")}
            }
            Row(Modifier.fillMaxWidth().padding(start=16.dp,end=10.dp),verticalAlignment=Alignment.CenterVertically){
                Text("左右にスライドして時代を選ぶ",Modifier.weight(1f),fontSize=11.sp,color=Muted)
                TextButton({info(pager.settledPage)},contentPadding=PaddingValues(horizontal=8.dp)){Text("地図の出典",fontSize=11.sp)}
            }
        }
    }
}

@Composable internal fun MapSourcesDialog(period:GeologicPeriod,art:Artwork,close:()->Unit){
    val context=LocalContext.current
    AlertDialog(onDismissRequest=close,title={Text("${period.label}の世界地図")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("${period.mapAge}の古地理復元図です。コレクションには、この時点に限らず${period.label}全体の生きものを表示します。",fontSize=13.sp)
            Text("大陸・海岸線は研究に基づく推定です。黒い線は位置の目安として重ねた現在の国境で、当時の国境ではありません。",fontSize=13.sp)
            Text("Scotese et al. · PALEOMAP（2024）\n作者：${art.artist}",fontSize=12.sp)
            Text("${art.license}。配信用の縮小画像を使用し、色の変更やトリミングは行っていません。",fontSize=12.sp)
            TextButton({openLink(context,art.source)}){Text("地図の原本・権利情報")}
            TextButton({openLink(context,art.licenseUrl)}){Text("地図のライセンス条件")}
            TextButton({openLink(context,"https://doi.org/10.5281/zenodo.10659112")}){Text("PALEOMAPの研究資料")}
            Text("時代区分の年代：ICS国際年代層序表 2026/06。境界年代は概数で、今後更新される場合があります。",fontSize=12.sp)
            TextButton({openLink(context,"https://stratigraphy.org/chart/")}){Text("時代区分の出典")}
        }
    },confirmButton={TextButton(close){Text("閉じる")}})
}
