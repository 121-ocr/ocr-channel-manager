package ocr.channel.organization;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * TODO: 渠道区域查询
 * @date 2016年11月15日
 * @author lijing
 */
public class ChannelRegionQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "find_regions";

	public ChannelRegionQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	/**
	 * 使用了分组查询区域
	 * db.getCollection('ba_sales_channels_3').aggregate(
		   [
		      {
		        $group : {
		           _id : { "region_code": "$region.code"},
                   region_code: {$first: "$region.code" },
		           region_name: {$first: "$region.full_name" }
		        }
		      },
		      {
		        $sort: {
		          region_code: 1
		        }
		      }
		   ]
		)	
	 * @param params
	 * @param next
	 */
	@Override
	public void handle(CommandMessage<JsonObject> msg) {	
		
	
		JsonObject groupComputeFields = new JsonObject()
											.put("_id", new JsonObject().put("region_code", "$region.code"))
											.put("region_code", new JsonObject().put("$first", "$region.code"))
											.put("region_name", new JsonObject().put("$first", "$region.full_name"));

		
		JsonObject sortObj = new JsonObject().put("$sort", new JsonObject().put("region_code", 1));

		
		JsonObject groupObj = new JsonObject().put("$group", groupComputeFields);
		
		JsonArray piplelineArray = new JsonArray();
		piplelineArray.add(groupObj).add(sortObj);
		
		JsonObject command = new JsonObject()
								  .put("aggregate", appActivity.getDBTableName(appActivity.getBizObjectType()))
								  .put("pipeline", piplelineArray);

		appActivity.getAppDatasource().getMongoClient().runCommand("aggregate", command, result -> {
		if (result.succeeded()) {
				JsonArray stockOnHandRet = result.result().getJsonArray("result");    	  
				msg.reply(stockOnHandRet);				

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
