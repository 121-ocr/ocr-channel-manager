package ocr.channel.supplyrelation;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
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
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject body = msg.body();
		
		String goodsOwner = this.appActivity.getAppInstContext().getAccount();		
		String warehouseAccount = body.getString("warehouse_account");
		
		JsonObject query = new JsonObject();
		query.put("goods_owner", goodsOwner);
		query.put("warehouse_account", warehouseAccount);
		
		appActivity.getAppDatasource().getMongoClient().find("bc_vmi_relations", 
				query, findRet->{
					if (findRet.succeeded()) {
						msg.reply(findRet.result());
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
