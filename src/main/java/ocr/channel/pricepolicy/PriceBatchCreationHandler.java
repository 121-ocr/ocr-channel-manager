package ocr.channel.pricepolicy;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 批量价格创建
 * 
 * @date 2016年11月20日
 * @author lijing
 */
// 业务活动功能处理器
public class PriceBatchCreationHandler extends ActionHandlerImpl<JsonArray> {

	public static final String ADDRESS = "batch_create";

	public PriceBatchCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonArray> msg) {
		JsonArray bodys = msg.getContent();

		// 现存量维度=商品SKU+商品租户id+批次号+货位编码+仓库编码+存量+冗余字段 {主键+租户id+货位集合+仓库集合+商品集合}
		//so.put(StockOnHandConstant.account, this.appActivity.getAppInstContext().getAccount());
		//JsonObject settingInfo = getParams(msg, so);
		appActivity.getAppDatasource().getMongoClient_oto().save(appActivity.getDBTableName(appActivity.getBizObjectType()),
				bodys, result -> {
					if (result.succeeded()) {
						//settingInfo.put("_id", result.result());
						msg.reply(result.result());
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});

	}


/*	private String stockOnHandNullVal(JsonObject so) {
		StringBuffer errors = new StringBuffer();
		
		if(!so.containsKey(StockOnHandConstant.status)){
			errors.append("状态");
		}
		
		if(!so.containsKey(StockOnHandConstant.biz_data_type)){
			errors.append("来源单据类型");
		}
		
		if(!so.containsKey(StockOnHandConstant.bo_id)){
			errors.append("来源单据ID");
		}
		
		Object locations = so.getValue(StockOnHandConstant.locationcode);

		if (null == locations || locations.equals("")) {
			errors.append("货位");
		}

		Object warehouses = so.getValue(StockOnHandConstant.warehouses);

		if (null == warehouses || warehouses.equals("")) {
			errors.append("仓库");
		}

		String sku = so.getString(StockOnHandConstant.sku);
		if (null == sku || sku.equals("")) {
			errors.append("sku");
		}
		
		String goodaccount = so.getString(StockOnHandConstant.goodaccount); 
		if (null == goodaccount || goodaccount.equals("")) {
			errors.append("商品所属货主");
		}
		
		Object goods = so.getValue(StockOnHandConstant.goods); 

		if (null == goods || goods.equals("")) {
			errors.append("商品信息");
		}
		
		if(!so.containsKey(StockOnHandConstant.onhandnum)){
			errors.append("现存量");
		}

		return errors.toString();
	}*/

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
