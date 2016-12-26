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
 * TODO: 补货仓库存量和价格查询
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
		
		
		//取价格，并返回是否有批次价格	
		JsonObject existBatchPriceQuery = new JsonObject().put("channel.account", to_account)
				.put("goods.product_sku_code", sku);
		String existBatchPriceSrvName = this.appActivity.getService().getRealServiceName();
		String existBatchPriceAddress = this.appActivity.getAppInstContext().getAccount() + "." + existBatchPriceSrvName + "." + "pricepolicy-mgr.exist_batchprice";							
		this.appActivity.getEventBus().send(existBatchPriceAddress,
				existBatchPriceQuery, existBatchPriceSrvRet->{
		if(existBatchPriceSrvRet.succeeded()){															
			JsonObject existBatchPriceRet = (JsonObject)existBatchPriceSrvRet.result().body();
			JsonArray priceRetArray = existBatchPriceRet.getJsonArray("results");
			Boolean existBatchPrice = existBatchPriceRet.getBoolean("exist_batch_price");
			
			JsonObject query = new JsonObject();
			query.put("to_warehouse_code", to_warehouse_code);
			query.put("to_account", to_account);
			query.put("from_account", from_account);
			
			appActivity.getAppDatasource().getMongoClient().find("bc_replenishment_relations", 
					query, findRet->{
						if (findRet.succeeded()) {
							
							List<JsonObject> replenishmentRelations = findRet.result();
							
							List<Future> futures = new ArrayList<>();
							
							for(JsonObject replenishmentRelation : replenishmentRelations){
								
								Future<JsonArray> repRelationFuture = Future.future();
								futures.add(repRelationFuture);						
								
								String whCode = replenishmentRelation.getString("from_warehouse_code");
								
								//取仓库档案							
								String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
								String getWarehouseAddress = from_account + "." + invSrvName + "." + "invorg-mgr.query_name";							
								this.appActivity.getEventBus().send(getWarehouseAddress,
								new JsonObject().put("code", whCode), whRet->{
									if(whRet.succeeded()){
										JsonObject whNameRet = (JsonObject)whRet.result().body();
										String whName = whNameRet.getString("name");
										
										JsonObject stockOnHandParams = new JsonObject();
										
										//构建现存量查询参数
										stockOnHandParams.put("query", new JsonObject().put("warehousecode", whCode)
																						.put("sku", sku));	
										
										//设置分组
										stockOnHandParams.put("group_keys", new JsonArray()
																					.add("warehousecode")
																					.add("sku")
																					.add("invbatchcode")
																					.add("shelf_life"));
										
										//设置参与计算的库存状态
										stockOnHandParams.put("status", new JsonArray()
																				.add("IN")
																				.add("OUT")
																				.add("RES"));
										//设置排序
										//stockOnHandParams.put("sort", new JsonObject().put("onhandnum", 1));	
										
										stockOnHandParams.put("sort",  new JsonObject()
																				.put("shelf_life", 1)
																				.put("invbatchcode", 1)
																				.put("onhandnum", 1));
										
										

										//取仓库现存量						
										//String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
										String getOnHandAddress = from_account + "." + invSrvName + "." + "stockonhand-mgr.query";							
										this.appActivity.getEventBus().send(getOnHandAddress,
												stockOnHandParams, invRet->{
												if(invRet.succeeded()){
													JsonArray stockNumArray = (JsonArray)invRet.result().body();
													//JsonObject retObjs = (JsonObject)invRet.result().body();
													//if(retObjs.containsKey("result")){
														//JsonArray stockNumArray = retObjs.getJsonArray("result");	
														if(stockNumArray == null || stockNumArray.size() <= 0){
															repRelationFuture.complete(stockNumArray);												
														}else{													
															/*if(existBatchPrice){*/
																
															    if(priceRetArray == null || priceRetArray.size() == 0){
															    	repRelationFuture.complete(stockNumArray);	
															    	return;
															    }
															    
															    
																List<Future> priceFutures = new ArrayList<>();
															    
																stockNumArray.forEach(item->{
																	JsonObject stockOnHandNumObj = (JsonObject)item;
																	JsonObject _idFields = stockOnHandNumObj.getJsonObject("_id");
																	_idFields.put("warehousename", whName);
																	
																	boolean priceMatched = false;
																	
																	for(Object item2: priceRetArray){
																		JsonObject priceItem = (JsonObject)item2;
																		//存在批次价格
																		if(existBatchPrice){
																			if(_idFields.getString("sku")
																					.equals(priceItem.getJsonObject("goods").getString("product_sku_code"))
																					&& _idFields.getString("invbatchcode")
																							.equals(priceItem.getString("invbatchcode"))){
																				stockOnHandNumObj.put("supply_price", priceItem.getJsonObject("supply_price"));
																				stockOnHandNumObj.put("retail_price", priceItem.getJsonObject("retail_price"));
																				stockOnHandNumObj.put("commission", priceItem.getJsonObject("commission"));
																				priceMatched = true;
																				break;
																			}	
																		}else{
																			//不存在批次价格
																			if(_idFields.getString("sku")
																					.equals(priceItem.getJsonObject("goods").getString("product_sku_code"))){
																				stockOnHandNumObj.put("supply_price", priceItem.getJsonObject("supply_price"));
																				stockOnHandNumObj.put("retail_price", priceItem.getJsonObject("retail_price"));
																				stockOnHandNumObj.put("commission", priceItem.getJsonObject("commission"));
																				priceMatched = true;
																				break;
																			}
																		}						
																		
																	}
																	
																	Future<Void> priceFuture = Future.future();
																	priceFutures.add(priceFuture);	
																	
																	if(!priceMatched){						
																		
																		JsonObject priceQuery = new JsonObject().put("goods.product_sku_code", sku)
																												.put("invbatchcode", _idFields.getString("invbatchcode"))
																												.put("channel", new JsonObject().put("$exists", false));

																		
																		String priceAddress = this.appActivity.getAppInstContext().getAccount() + "." + existBatchPriceSrvName + "." + "pricepolicy-mgr.findall";							
																		this.appActivity.getEventBus().send(priceAddress,
																				priceQuery, priceSrvRet->{
																			if(priceSrvRet.succeeded()){															
																				JsonObject priceRets = (JsonObject)priceSrvRet.result().body();
																				JsonObject priceRet = priceRets.getJsonArray("result").getJsonObject(0);
																				stockOnHandNumObj.put("supply_price", priceRet.getJsonObject("supply_price"));
																				stockOnHandNumObj.put("retail_price", priceRet.getJsonObject("retail_price"));
																				stockOnHandNumObj.put("commission", priceRet.getJsonObject("commission"));
																				
																				priceFuture.complete();
																			}else{																				
																				priceFuture.fail(priceSrvRet.cause());
																			}
																			
																		});
																	}else{
																		priceFuture.complete();
																	}

																	
																});
																
																CompositeFuture.join(priceFutures).setHandler(ar -> {																
																	repRelationFuture.complete(stockNumArray);																
																});
																
															/*}else{
																//不存在批次价格															
																
															    if(priceRetArray == null || priceRetArray.size() == 0){
															    	repRelationFuture.complete(stockNumArray);	
															    	return;
															    }
																stockNumArray.forEach(item->{
																	JsonObject stockOnHandNumObj = (JsonObject)item;
																	JsonObject _idFields = stockOnHandNumObj.getJsonObject("_id");
																	_idFields.put("warehousename", whName);
																	priceRetArray.forEach(item2->{
																		JsonObject priceItem = (JsonObject)item2;
																		if(_idFields.getString("sku")
																				.equals(priceItem.getJsonObject("goods").getString("product_sku_code"))){
																			stockOnHandNumObj.put("supply_price", priceItem.getJsonObject("supply_price"));
																			stockOnHandNumObj.put("retail_price", priceItem.getJsonObject("retail_price"));
																			stockOnHandNumObj.put("commission", priceItem.getJsonObject("commission"));
																		}																	
																	});
																	
																});
																
																repRelationFuture.complete(stockNumArray);																
																
															}													*/		

														}
													/*}else{				
														repRelationFuture.complete(retObjs);
													}*/
												}else{			
													Throwable err = invRet.cause();
													String errMsg = err.getMessage();
													appActivity.getLogger().error(errMsg, err);		
													
													repRelationFuture.fail(err);
												}
												
											});										
										
										
									
									}else{
										Throwable err = whRet.cause();
										String errMsg = err.getMessage();
										appActivity.getLogger().error(errMsg, err);
										msg.fail(500, errMsg);
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
											JsonArray skuStocks = comFutures.result(i);
											//JsonArray skuStocks = itemObject.getJsonArray("result");
											if(skuStocks != null && skuStocks.size() > 0){
												for(Object item: skuStocks){
													JsonObject tmpObj = (JsonObject)item;
													JsonObject skuStock = tmpObj.getJsonObject("_id");
													Double currentOnhand = tmpObj.getDouble("onhandnum");
													onHandNum += currentOnhand;
													JsonObject retItemObject = new JsonObject().put("warehousecode", skuStock.getString("warehousecode"))
															.put("warehousename", skuStock.getString("warehousename"))
															.put("invbatchcode", skuStock.getString("invbatchcode"))
															.put("shelf_life", skuStock.getString("shelf_life"))
															.put("onhandnum", currentOnhand);
													if(tmpObj.containsKey("supply_price"))
														retItemObject.put("supply_price", tmpObj.getJsonObject("supply_price"));
													if(tmpObj.containsKey("retail_price"))
														retItemObject.put("retail_price", tmpObj.getJsonObject("retail_price"));
													if(tmpObj.containsKey("commission"))
														retItemObject.put("commission", tmpObj.getJsonObject("commission"));
													
													retArray.add(retItemObject);
												}
											}
										}
									}																			
								}
								msg.reply(new JsonObject().put("total", onHandNum)
														  .put("exist_batch_price", existBatchPrice)
														  .put("sub_nums", retArray));
							});					
	
							
						} else {
							Throwable err = findRet.cause();
							String errMsg = err.getMessage();
							appActivity.getLogger().error(errMsg, err);
							msg.fail(500, errMsg);
						}
						
					});		
			
				}else{
					Throwable err = existBatchPriceSrvRet.cause();
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
