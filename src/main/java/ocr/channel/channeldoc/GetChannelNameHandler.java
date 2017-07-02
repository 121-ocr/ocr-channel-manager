package ocr.channel.channeldoc;

import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

/**
 * 查询渠道档案名称
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class GetChannelNameHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "query_name";

	public GetChannelNameHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {

		JsonObject query = msg.getContent();
		
		FindOptions findOptions = new FindOptions();	
		findOptions.setFields(new JsonObject().put("name", true));

		appActivity.getAppDatasource().getMongoClient()
				.findWithOptions(appActivity.getDBTableName(this.appActivity.getBizObjectType()), query, findOptions, findRet -> {
					if (findRet.succeeded()) {
						List<JsonObject> retObj = findRet.result();
						if(retObj != null && retObj.size() > 0)
						{
							msg.reply(retObj.get(0));
						}else{
							msg.reply(null);
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

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
