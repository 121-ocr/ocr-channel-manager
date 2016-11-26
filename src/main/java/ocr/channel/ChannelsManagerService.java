package ocr.channel;

import java.util.ArrayList;
import java.util.List;

import ocr.channel.organization.ChannelOrganizationComponent;
import otocloud.framework.app.engine.AppServiceImpl;
import otocloud.framework.app.engine.WebServer;
import otocloud.framework.app.function.AppActivity;
import otocloud.framework.app.function.AppInitActivityImpl;

/**
 * TODO: 渠道管理微服务
 * @date 2016年11月26日
 * @author lijing@yonyou.com
 */
public class ChannelsManagerService extends AppServiceImpl
{

	//创建服务初始化组件
	@Override
	public AppInitActivityImpl createAppInitActivity() {		
		return null;
	}

	//创建租户级web server
	@Override
	public WebServer createWebServer() {
		// TODO Auto-generated method stub
		return null;
	}

	//创建服务内的业务活动组件
	@Override
	public List<AppActivity> createBizActivities() {
		List<AppActivity> retActivities = new ArrayList<>();
		
/*		CatelogManagementComponent catelogCom = new CatelogManagementComponent();
		retActivities.add(catelogCom);
				
		ProductManagementComponent productCom = new ProductManagementComponent();
		retActivities.add(productCom);
		
		SalesOrderComponent soCom = new SalesOrderComponent();
		retActivities.add(soCom);*/
		
		ChannelOrganizationComponent channelOrganizationComponent = new ChannelOrganizationComponent();
		retActivities.add(channelOrganizationComponent);

		return retActivities;
	}
}
