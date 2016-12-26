package ocr.channel.pricepolicy;

import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 是否存在批次价格
 * @date 2016年11月15日
 * @author lijing
 */
public class ExistBatchPricePolicyHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "exist_batchprice";

	public ExistBatchPricePolicyHandler(AppActivityImpl appActivity) {
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
     * 查询SKU渠道价格表
     * {
     * 		"channel.account": to_account,
     * 		"goods.product_sku_code": sku
     * }
    */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject query = msg.body();
		
/*		JsonObject query = new JsonObject().put("channel.account", targetAccount)
				.put("goods.product_sku_code", sku);*/
		
/*		FindOptions findOptions = new FindOptions();	
		findOptions.setFields(new JsonObject().put("invbatchcode", true)
											  .put("invbatchcode", true));		*/

		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()), 
				query, result -> {
					if (result.succeeded()) {
						List<JsonObject> results = result.result();
						if(results != null && results.size() > 0){
							for(Object item : results){
								JsonObject itemObj = (JsonObject)item;
								if(itemObj.containsKey("invbatchcode")){
									String invbatchcode = itemObj.getString("invbatchcode");
									if(invbatchcode != null && !invbatchcode.isEmpty()){
										msg.reply(new JsonObject().put("exist_batch_price", true)
													.put("results", results));
										return;
									}
								}
							}
							msg.reply(new JsonObject().put("exist_batch_price", false)
									.put("results", results));
							return;
						}else{
							//取不区分渠道的统一供货价格
							query.remove("channel.account");
							appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(appActivity.getBizObjectType()), 
									query, newResult -> {
										if (newResult.succeeded()) {
											List<JsonObject> newResults = newResult.result();
											if(newResults != null && newResults.size() > 0){
												msg.reply(new JsonObject().put("exist_batch_price", false)
														.put("results", newResults));
												
												
											}else{
												msg.fail(100, "无价格数据");
											}
											
										}else{
											Throwable errThrowable = newResult.cause();
											String errMsgString = errThrowable.getMessage();
											appActivity.getLogger().error(errMsgString, errThrowable);
											msg.fail(100, errMsgString);
										}
									});
							
							
						}

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
		paramsDesc.add(new ApiParameterDescriptor("target_account",""));		
		paramsDesc.add(new ApiParameterDescriptor("sku",""));
		
		actionDescriptor.getHandlerDescriptor().setParamsDesc(paramsDesc);	*/
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	
	
}
