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
 * TODO: 补货仓库查询
 * @date 2016年11月15日
 * @author lijing
 */
public class ReplenishmentRelationsQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "bc_replenishment_relations.get";

	public ReplenishmentRelationsQueryHandler(AppActivityImpl appActivity) {
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
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject body = msg.body();
		
		String to_warehouse_code = body.getString("to_warehouse_code");
		String to_account = body.getString("to_account");
		String from_account = this.appActivity.getAppInstContext().getAccount();
		
		JsonObject query = new JsonObject();
		query.put("to_warehouse_code", to_warehouse_code);
		query.put("to_account", to_account);
		query.put("from_account", from_account);
		
		appActivity.getAppDatasource().getMongoClient().find("bc_replenishment_relations", 
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
