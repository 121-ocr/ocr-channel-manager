package ocr.channel.organization;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import ocr.common.handler.SampleSingleDocQueryHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * TODO: 渠道树查询
 * @date 2016年11月15日
 * @author lijing
 */
public class ChannelTreeQueryHandler extends SampleSingleDocQueryHandler {
	
	public static final String ADDRESS = "findtree";

	public ChannelTreeQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		
		JsonObject queryObj = new JsonObject();
		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()),
				queryObj, result -> {
					if (result.succeeded()) {
						List<JsonObject> parent = new ArrayList<>();
						if(result.result().size() > 0){
							List<JsonObject> list = result.result();
							List<JsonObject> treeList = new ArrayList<>();
							for(JsonObject json:list){
								JsonObject tmp = new JsonObject();
								tmp.put("id", json.getString("code"));
								tmp.put("text", json.getString("name"));
								tmp.put("attributes", json);
								treeList.add(tmp);
							}
							
							List<JsonObject> children = new ArrayList<>();
							for(JsonObject json:treeList){
								if(json.getJsonObject("attributes").getJsonObject("parentid").isEmpty()){
									parent.add(json);
								}else{
									children.add(json);
								}
							}
							if(parent.size() > 0){
								for(JsonObject par:parent){
									
//									par.put("state", "closed");
									String parentId = par.getJsonObject("attributes").getString("_id");
									for(JsonObject child:children){
										String childParentId = child.getJsonObject("attributes").getJsonObject("parentid").getString("_id");
										if(parentId.equals(childParentId)){
											if(par.containsKey("children")){
												par.put("state", "closed");
												par.getJsonArray("children").add(child);
											}else{
												List<JsonObject> t = new ArrayList<>();
												t.add(child);
												par.put("children", new JsonArray(t));
												par.put("state", "closed");
											}
											
											
										}
									}
									treeList.add(par);
								}
							}
							
						}
						msg.reply(new JsonArray(parent));
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});



	}
	

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
		//handlerDescriptor.setMessageFormat("command");
		
		//参数
/*		List<ApiParameterDescriptor> paramsDesc = new ArrayList<ApiParameterDescriptor>();
		paramsDesc.add(new ApiParameterDescriptor("targetacc",""));		
		paramsDesc.add(new ApiParameterDescriptor("soid",""));		
		
		actionDescriptor.getHandlerDescriptor().setParamsDesc(paramsDesc);	*/
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.GET);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	
	
}
