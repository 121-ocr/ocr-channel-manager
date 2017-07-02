package ocr.channel.supplyrelation;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * TODO: 补货仓库查询
 * @date 2016年11月15日
 * @author lijing
 */
public class ReplenishmentWarehousesQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "bc_replenishment_warehouses.get";

	public ReplenishmentWarehousesQueryHandler(AppActivityImpl appActivity) {
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
     * 查询渠道仓库的补货仓库：
     * 1、传入渠道仓库ID、渠道仓库所属的企业租户ID
     * 2、在仓库补货关系(bc_replenishment_relations)中查询补货的来源仓库（本企业），针对于一个渠道仓库，可能有多个补货仓库
     */
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		JsonObject body = msg.getContent();
		
		String to_warehouse_code = body.getString("to_warehouse_code");
		String to_account = body.getString("to_account");
		String account = this.appActivity.getAppInstContext().getAccount();
		
		JsonObject query = new JsonObject();
		query.put("to_warehouse_code", to_warehouse_code);
		query.put("to_account", to_account);
		query.put("from_account", account);
		
		appActivity.getAppDatasource().getMongoClient().find("bc_replenishment_relations", 
				query, findRet->{
					if (findRet.succeeded()) {
						//msg.reply(findRet.result());
						
						List<JsonObject> replenishmentRelations = findRet.result();
						
						if(replenishmentRelations != null && replenishmentRelations.size() > 0){
							
							//构建仓库档案查询条件
							JsonObject queryInv = new JsonObject();
							if(replenishmentRelations.size() == 1){
								queryInv.put("code", replenishmentRelations.get(0).getString("from_warehouse_code"));
							}else{
								JsonArray queryItems = new JsonArray();
								queryInv.put("$or", queryItems);								
								for(JsonObject vmiRelation : replenishmentRelations){
									queryItems.add(new JsonObject().put("code", vmiRelation.getString("from_warehouse_code")));
								}
							}							
							
							//取仓库档案							
							String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
							String getWarehouseAddress = account + "." + invSrvName + "." + "warehouse-mgr.query";							
							this.appActivity.getEventBus().send(getWarehouseAddress,
									queryInv, invRet->{
										if(invRet.succeeded()){
											JsonObject ret = (JsonObject)invRet.result().body();
											if(ret == null){
												msg.reply(replenishmentRelations);
												return;
											}
											JsonArray warehouses = ret.getJsonArray("result");
											if(warehouses != null && warehouses.size() > 0){							
												if(replenishmentRelations.size() == 1){
													replenishmentRelations.get(0).put("ba_warehouses", warehouses.getJsonObject(0));
												}else{													
													for(JsonObject vmiRelation : replenishmentRelations){
														String whCode = vmiRelation.getString("from_warehouse_code");
														for(Object item : warehouses){
															JsonObject warehouse = (JsonObject)item;
															if(warehouse.getString("code").equals(whCode)){
																vmiRelation.put("ba_warehouses", warehouse);
																break;
															}
														}
													}
												}											
											}	
											msg.reply(replenishmentRelations);											
										}else{
											Throwable err = invRet.cause();
											String errMsg = err.getMessage();
											appActivity.getLogger().error(errMsg, err);
											
											msg.reply(replenishmentRelations);
										}								
										
										
							});	
							
							
							
						}					
						
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
