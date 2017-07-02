package ocr.channel.supplyrelation;

import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: VMI仓库查询
 * @date 2016年11月15日
 * @author lijing
 */
public class VMIWarehouseQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "bc_vmi_relations.get";

	public VMIWarehouseQueryHandler(AppActivityImpl appActivity) {
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
     * 查询补货关系：
     * 1、传入渠道租户ID，和企业租户ID组合成条件
     * 2、在VMI关系表(bc_vmi_relations)中查询货主是本企业的渠道仓库
     */
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		JsonObject body = msg.getContent();
		
		String goodsOwner = this.appActivity.getAppInstContext().getAccount();		
		String warehouseAccount = body.getString("warehouse_account");
		
		JsonObject query = new JsonObject();
		query.put("goods_owner", goodsOwner);
		query.put("warehouse_account", warehouseAccount);
		
		appActivity.getAppDatasource().getMongoClient().find("bc_vmi_relations", 
				query, findRet->{
					if (findRet.succeeded()) {
					
						List<JsonObject> vmiRelations = findRet.result();
						if(vmiRelations != null && vmiRelations.size() > 0){	
							//构建仓库档案查询条件
							JsonObject queryInv = new JsonObject();
							if(vmiRelations.size() == 1){
								queryInv.put("code", vmiRelations.get(0).getString("warehouse_code"));
							}else{
								JsonArray queryItems = new JsonArray();
								queryInv.put("$or", queryItems);								
								for(JsonObject vmiRelation : vmiRelations){
									queryItems.add(new JsonObject().put("code", vmiRelation.getString("warehouse_code")));
								}
							}							
							
							//取仓库档案							
							String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
							String getWarehouseAddress = warehouseAccount + "." + invSrvName + "." + "warehouse-mgr.query";							
							this.appActivity.getEventBus().send(getWarehouseAddress,
									queryInv, invRet->{
										if(invRet.succeeded()){
											JsonObject ret = (JsonObject)invRet.result().body();
											if(ret == null){
												msg.reply(vmiRelations);
												return;
											}
											JsonArray warehouses = ret.getJsonArray("result");
											if(warehouses != null && warehouses.size() > 0){							
												if(vmiRelations.size() == 1){
													vmiRelations.get(0).put("ba_warehouses", warehouses.getJsonObject(0));
												}else{													
													for(JsonObject vmiRelation : vmiRelations){
														String whCode = vmiRelation.getString("warehouse_code");
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
											msg.reply(vmiRelations);											
										}else{
											Throwable err = invRet.cause();
											String errMsg = err.getMessage();
											appActivity.getLogger().error(errMsg, err);
											
											msg.reply(vmiRelations);
										}								
										
										
							});	
						}else{
							msg.reply(vmiRelations);
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
