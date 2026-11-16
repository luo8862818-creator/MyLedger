package com.myledger.app.ui.transaction

import android.Manifest
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.myledger.app.domain.input.*
import com.myledger.app.data.entity.Transaction
import com.myledger.app.ui.components.*
import kotlinx.coroutines.*

@Composable fun SmartEntryInput(mode:String?,onModeChange:(String?)->Unit,isSaving:Boolean,onSave:(List<Transaction>)->Unit,allowMultiple:Boolean=true,modifier:Modifier=Modifier){
 val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle;val scope=rememberCoroutineScope();val rules=remember{CategoryRules(context)}
 val factory=LocalLedgerSpeechFactory.current;val engine=remember(factory,context){factory(context)}
 val keyboard=LocalSoftwareKeyboardController.current
 var session by remember{mutableIntStateOf(0)};var text by remember{mutableStateOf("")};var status by remember{mutableStateOf("")}
 var listening by remember{mutableStateOf(false)};var processing by remember{mutableStateOf(false)};var parsing by remember{mutableStateOf(false)}
 var levels by remember{mutableStateOf(List(36){0f})};var problem by remember{mutableStateOf("")};var committing by remember{mutableStateOf(false)}
 val latestSave by rememberUpdatedState(onSave);val latestMode by rememberUpdatedState(mode)
 fun parseInput(input:String)=LedgerBatchParser.parse(input,customExpense=rules.read("支出"),customIncome=rules.read("收入"))
 val preview by produceState<ParsedLedgerBatch?>(null,text){value=null;if(text.isNotBlank()){delay(200);value=withContext(Dispatchers.Default){parseInput(text)}}}
 fun cancel(){session++;listening=false;processing=false;engine.cancel();levels=List(36){0f}}
 fun save(input:String=text){
  if(parsing||isSaving||committing||input.isBlank())return
  cancel();val run=session;parsing=true;problem=""
  scope.launch{try{
   val p=withContext(Dispatchers.Default){parseInput(input)}
   if(run!=session)return@launch
   if(!p.readyToSave){problem=p.problems.joinToString("；")+"。请修改上面的描述后再保存";status="需要补充信息";return@launch}
   if(!allowMultiple&&p.entries.size!=1){problem="编辑账单只能修改一笔，请回到新增记账添加多笔";return@launch}
   committing=true;keyboard?.hide()
   latestSave(p.entries.map{entry->Transaction(amountCent=checkNotNull(entry.amountCent),type=checkNotNull(entry.type),category=checkNotNull(entry.category),note=entry.note,date=entry.date?:System.currentTimeMillis())})
  }finally{parsing=false}}
 }
 fun begin(){
  if(isSaving||committing)return
  cancel();val run=session;problem=""
  if(!engine.available){status="此设备没有可用语音服务，可直接输入文字保存";return}
  try{listening=true;status="正在连接麦克风…"
   engine.start(onReady={if(run==session){listening=true;processing=false;status="正在聆听，说完后自动记账"}},
    onLevel={if(run==session&&listening)levels=levels.drop(1)+((it+2f)/12f).coerceIn(0f,1f)},
    onPartial={if(run==session&&latestMode=="voice")text=it},
    onFinal={if(run==session&&latestMode=="voice"){listening=false;processing=false;if(it.isBlank())status="没有听清，请重试或输入文字"else{text=it;status="正在保存账单…";save(it)}}},
    onError={if(run==session){listening=false;processing=false;status=when(it){SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"请允许麦克风权限，或直接输入文字";SpeechRecognizer.ERROR_NETWORK,SpeechRecognizer.ERROR_NETWORK_TIMEOUT->"语音网络连接失败，可修改文字后保存";else->"没有听清，请重试或直接输入文字"}}},
    onEnd={if(run==session){listening=false;processing=true;status="正在识别…"}})
  }catch(_:Exception){listening=false;processing=false;status="语音服务暂不可用，可直接输入文字"}
 }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it&&latestMode=="voice")begin()else status="未允许麦克风，可直接输入文字保存"}
 LaunchedEffect(mode){cancel();problem="";if(mode=="voice"){keyboard?.hide();permission.launch(Manifest.permission.RECORD_AUDIO)}}
 LaunchedEffect(isSaving,committing){if(committing&&!isSaving){withFrameNanos{};committing=false}}
 DisposableEffect(lifecycle,engine){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP){cancel();status="录音已暂停，文字已保留"}};lifecycle.addObserver(observer);onDispose{session++;lifecycle.removeObserver(observer);engine.cancel();engine.destroy()}}
 if(mode==null){Row(modifier,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  FilledTonalButton(onClick={onModeChange("text")},modifier=Modifier.weight(1f)){LedgerIcon("edit",Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text("一句话记账")}
  FilledTonalButton(onClick={onModeChange("voice")},modifier=Modifier.weight(1f)){LedgerIcon("mic",Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text("语音记账")}
 };return}
 Column(modifier.fillMaxSize().testTag("inline-smart-entry")){
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(enabled=!isSaving&&!committing,onClick={cancel();onModeChange(null)}){Text("手动记账")};TextButton(enabled=!isSaving&&!committing,onClick={cancel();onModeChange(if(mode=="voice")"text"else "voice")}){Text(if(mode=="voice")"改用文字"else "改用语音")}}
   Text(if(mode=="voice")"例如：吃饭20，打车30。识别完整后自动分成两笔保存。"else "例如：吃饭20，打车30。自动拆分账单，核对后一次保存。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(mode=="voice")Surface(color=Color(0xFF192F3E),shape=MaterialTheme.shapes.large){Column(Modifier.fillMaxWidth().padding(16.dp)){
    Text(if(listening)"正在聆听"else if(processing||parsing)"正在识别"else "语音记账",color=Color.White,style=MaterialTheme.typography.titleSmall)
    SiriWaveform(levels,listening,processing||parsing)
    Text(status,style=MaterialTheme.typography.bodySmall,color=Color(0xFFD2E4E8))
    Row{if(listening)TextButton(onClick={engine.stop();listening=false;processing=true;status="正在识别…"}){Text("结束说话",color=Color(0xFF8CE7D2))}else TextButton(enabled=!processing&&!parsing&&!isSaving&&!committing,onClick={permission.launch(Manifest.permission.RECORD_AUDIO)}){Text("重新录入",color=Color(0xFF8CE7D2))};if(listening||processing)TextButton(onClick={cancel();status="录音已取消，文字已保留"}){Text("取消录音",color=Color(0xFFD2E4E8))}}
   }}
   OutlinedTextField(value=text,onValueChange={text=it;problem=""},enabled=!isSaving&&!committing,readOnly=listening||processing,label={Text(if(mode=="voice")"识别文字，可修改"else "描述这笔账")},placeholder={Text("吃饭20")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4,shape=MaterialTheme.shapes.medium)
   preview?.let{SmartEntryPreview(it)}
   if(problem.isNotBlank())Text(problem,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag("smart-entry-error"))
   Spacer(Modifier.height(8.dp))
  }
  Button(enabled=text.isNotBlank()&&!listening&&!processing&&!parsing&&!isSaving&&!committing,onClick={save()},modifier=Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=14.dp).heightIn(min=54.dp),shape=MaterialTheme.shapes.medium){Text(if(parsing||isSaving||committing)"正在保存…"else if((preview?.entries?.size?:0)>1)"保存 ${preview!!.entries.size} 笔账单"else "保存账单")}
 }
}
