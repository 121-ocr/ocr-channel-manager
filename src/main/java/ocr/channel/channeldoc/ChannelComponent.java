package ocr.channel.channeldoc;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 渠道档案
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class ChannelComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return "channel-mgr";
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_sales_channels";
	}


	// 发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		return null;
	}

	// 业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {

		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		ChannelTreeQueryHandler channelTreeQueryHandler = new ChannelTreeQueryHandler(this);
		ret.add(channelTreeQueryHandler);
		
		ChannelRegionQueryHandler channelRegionQueryHandler = new ChannelRegionQueryHandler(this);
		ret.add(channelRegionQueryHandler);
		
		GetChannelNameHandler getChannelNameHandler = new GetChannelNameHandler(this);
		ret.add(getChannelNameHandler);

		ChannelCreateHandler createHandler = new ChannelCreateHandler(this);
		ret.add(createHandler);
		
		ChannelQueryHandler queryHandler2 = new ChannelQueryHandler(this);
		ret.add(queryHandler2);
		
		ChannelQueryNoPagingHandler channelQueryNoPagingHandler = new ChannelQueryNoPagingHandler(this);
		ret.add(channelQueryNoPagingHandler);
		
		ChannelUpdateHandler updateHandler = new ChannelUpdateHandler(this);
		ret.add(updateHandler);
		
		ChannelRemoveHandler removeHandler = new ChannelRemoveHandler(this);
		ret.add(removeHandler);

		return ret;
	}

}
