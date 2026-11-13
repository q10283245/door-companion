import org.json.*;import java.nio.file.*;import wb.ModelClient;
public class ModelProbe {public static void main(String[] args)throws Exception{
 JSONObject c=new JSONObject(Files.readString(Path.of(args[0])));c.put("timeout",20);c.put("persona","你是随车语音伙伴。根据事件和已确认的数据给一句40字以内的自然问候。不要复述普通提示。不要编造目的地或未知车况。只输出纯文本。");
 JSONArray results=new JSONArray();String[] scenes={"主驾刚上车关门，早上8点，外温22摄氏度，油量60%，全部为虚构测试数据","主驾准备下车开门，晚上9点，未提供位置和温度，全部为虚构测试数据","主驾刚关门，温度、油量和位置都未知，全部为虚构测试数据"};
 for(String scene:scenes){JSONObject context=new JSONObject().put("synthetic",true).put("scene",scene);long t=System.nanoTime();String response=ModelClient.request(c,context,"主驾车门状态已变化");JSONObject result=new JSONObject().put("scene",scene).put("text",response).put("elapsed_ms",(System.nanoTime()-t)/1000000).put("nonempty",!response.trim().isEmpty()).put("within_100_chars",response.length()<=100);results.put(result);System.out.println(result);if(response.trim().isEmpty())throw new Exception("Empty output");}
 Files.writeString(Path.of(args[1]),new JSONObject().put("model",c.getString("model")).put("synthetic_only",true).put("results",results).toString(2));}}
