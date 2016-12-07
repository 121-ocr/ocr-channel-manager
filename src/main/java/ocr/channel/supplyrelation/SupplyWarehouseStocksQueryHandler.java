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
							
							//取仓库现存量						
							String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
							String getWarehouseAddress = from_account + "." + invSrvName + "." + "stockonhand-mgr.query";							
							this.appActivity.getEventBus().send(getWarehouseAddress,
								invStocksQuery, invRet->{
									if(invRet.succeeded()){
										JsonObject retObjs = (JsonObject)invRet.result().body();
										if(retObjs.containsKey("result")){
											JsonArray stockNumArray = retObjs.getJsonArray("result");	
											if(stockNumArray == null || stockNumArray.size() <= 0){
												repRelationFuture.complete(retObjs);												
											}else{
										
												//构建取价格参数
												JsonArray queryPriceCondItems = new JsonArray();
												stockNumArray.forEach(item->{
													JsonObject stockOnHandNumObj = (JsonObject)item;
													queryPriceCondItems.add(new JsonObject().put("channel.account", to_account)
															.put("goods.product_sku_code", stockOnHandNumObj.getJsonObject("goods").getString("product_sku_code"))
															.put("invbatchcode", stockOnHandNumObj.getString("invbatchcode")));												
													
												});
												
												JsonObject queryPriceCondObject = new JsonObject();
												queryPriceCondObject.put("$or", queryPriceCondItems);
												
												//批量取价格					
												String priceSrvName = this.appActivity.getService().getRealServiceName();
												String priceAddress = this.appActivity.getAppInstContext().getAccount() + "." + priceSrvName + "." + "pricepolicy-mgr.findall";							
												this.appActivity.getEventBus().send(priceAddress,
														queryPriceCondObject, priceSrvRet->{
														if(priceSrvRet.succeeded()){
															//将SKU价格和存量组合
															JsonObject priceRetObj = (JsonObject)priceSrvRet.result().body();
															if(priceRetObj.containsKey("result")){
																JsonArray priceRetArray = priceRetObj.getJsonArray("result");											
																stockNumArray.forEach(item->{
																	JsonObject stockOnHandNumObj = (JsonObject)item;
																	priceRetArray.forEach(item2->{
																		JsonObject priceItem = (JsonObject)item2;
																		if(stockOnHandNumObj.getJsonObject("goods").getString("product_sku_code")
																				.equals(priceItem.getJsonObject("goods").getString("product_sku_code"))
																				&& stockOnHandNumObj.getString("invbatchcode")
																				.equals(priceItem.getString("invbatchcode"))){
																			stockOnHandNumObj.put("supply_price", priceItem.getJsonObject("supply_price"));
																			stockOnHandNumObj.put("retail_price", priceItem.getJsonObject("retail_price"));
																			stockOnHandNumObj.put("commission", priceItem.getJsonObject("commission"));
																		}																	
																	});															
																	
																	
																});
															}														
															
														}else{
															Throwable err = priceSrvRet.cause();
															String errMsg = err.getMessage();
															appActivity.getLogger().error(errMsg, err);													
	
														}
														
														repRelationFuture.complete(retObjs);
													});	
											}
										}else{				
											repRelationFuture.complete(retObjs);
										}
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
											for(Object item: skuStocks){
												JsonObject skuStock = (JsonObject)item;
												Double currentOnhand = skuStock.getDouble("onhandnum");
												onHandNum += currentOnhand;
												JsonObject retItemObject = new JsonObject().put("warehouses", skuStock.getJsonObject("warehouses"))
														.put("invbatchcode", skuStock.getString("invbatchcode"))
														.put("shelf_life", skuStock.getString("shelf_life"))
														.put("onhandnum", currentOnhand);
												if(skuStock.containsKey("supply_price"))
													retItemObject.put("supply_price", skuStock.getJsonObject("supply_price"));
												if(skuStock.containsKey("retail_price"))
													retItemObject.put("retail_price", skuStock.getJsonObject("retail_price"));
												if(skuStock.containsKey("commission"))
													retItemObject.put("commission", skuStock.getJsonObject("commission"));
												
												retArray.add(retItemObject);
											}
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
