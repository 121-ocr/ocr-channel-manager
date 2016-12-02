package ocr.channel.supplyrelation;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 补货仓库存量查询
 * @date 2016年11月15日
 * @author lijing
 */
public class SupplyWarehouseStocksQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "warehousestocks.get";

	public SupplyWarehouseStocksQueryHandler(AppActivityImpl appActivity) {
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
     * 查询补货仓库存量：
     * 1、传入渠道仓库ID（to）、渠道仓库所属的企业租户ID、需要补货的商品SKU
     * 2、在仓库补货关系(bc_replenishment_relations)中查询补货的来源仓库（本企业），针对于一个渠道仓库，可能有多个补货仓库
     *  var condStr = {
	        to_account: 渠道租户ID,
	        to_warehouse_code: 渠道仓库code,
	        sku: 需要补货的商品sku
    	}
     */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject body = msg.body();
		
		String to_warehouse_code = body.getString("to_warehouse_code");
		String to_account = body.getString("to_account");
		String sku = body.getString("sku");
		String from_account = this.appActivity.getAppInstContext().getAccount();
		
		JsonObject query = new JsonObject();
		query.put("to_warehouse_code", to_warehouse_code);
		query.put("to_account", to_account);
		query.put("from_account", from_account);
		
		appActivity.getAppDatasource().getMongoClient().find("bc_replenishment_relations", 
				query, findRet->{
					if (findRet.succeeded()) {
						//msg.reply(findRet.result());
						List<JsonObject> replenishmentRelations = findRet.result();
						
						List<Future> futures = new ArrayList<>();
						
						for(JsonObject replenishmentRelation : replenishmentRelations){
							
							Future<JsonObject> repRelationFuture = Future.future();
							futures.add(repRelationFuture);						
							
							String whCode = replenishmentRelation.getString("from_warehouse_code");
							
							JsonObject invStocksQuery = new JsonObject();
							invStocksQuery.put("warehousecode", whCode);
							invStocksQuery.put("sku", sku);
							
							//取仓库档案							
							String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
							String getWarehouseAddress = from_account + "." + invSrvName + "." + "stockonhand_mgr.query";							
							this.appActivity.getEventBus().send(getWarehouseAddress,
								invStocksQuery, invRet->{
									if(invRet.succeeded()){
										//List<JsonObject> retObjs = (List<JsonObject>)invRet.result().body();
										repRelationFuture.complete((JsonObject)invRet.result().body());
									}else{										
										repRelationFuture.fail(invRet.cause());
									}
									
								});	
						}						
						CompositeFuture.join(futures).setHandler(ar -> {
						  	JsonArray retArray =new JsonArray();
						  	Double onHandNum = 0.00;
							CompositeFutureImpl comFutures = (CompositeFutureImpl)ar;
							if(comFutures.size() > 0){										
								for(int i=0;i<comFutures.size();i++){
									if(comFutures.succeeded(i)){
										JsonObject itemObject = comFutures.result(i);
										JsonArray skuStocks = itemObject.getJsonArray("result");
										if(skuStocks != null && skuStocks.size() > 0){
											JsonObject skuStock = skuStocks.getJsonObject(0);
											Double currentOnhand = skuStock.getDouble("onhandnum");
											onHandNum += currentOnhand;
											retArray.add(new JsonObject().put("warehouses", skuStock.getJsonObject("warehouses"))
													.put("onhandnum", currentOnhand));
										}
									}
								}																			
							}
							msg.reply(new JsonObject().put("total", onHandNum)
													  .put("sub_nums", retArray));
						});					

						
					} else {
						Throwable err = findRet.cause();
						String errMsg = err.getMessage();
						appActivity.getLogger().error(errMsg, err);
						msg.fail(500, errMsg);
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
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	
	
}
