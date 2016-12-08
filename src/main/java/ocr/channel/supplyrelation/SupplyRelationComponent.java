package ocr.channel.supplyrelation;


import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * TODO: 补货关系管理
 * @date 2016年11月15日
 * @author lijing
 */
public class SupplyRelationComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "supplyrelation-mgr";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "";
	}

	//发布此业务活动关联的业务角色
	@Override
	public List<BizRoleDescriptor> exposeBizRolesDesc() {
		// TODO Auto-generated method stub
		BizRoleDescriptor bizRole = new BizRoleDescriptor("2", "核心企业");
		
		List<BizRoleDescriptor> ret = new ArrayList<BizRoleDescriptor>();
		ret.add(bizRole);
		return ret;
	}

	//发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		// TODO Auto-generated method stub
		return null;
	}


	//业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {
		
		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		VMIWarehouseQueryHandler vmiWarehouseQueryHandler = new VMIWarehouseQueryHandler(this);
		ret.add(vmiWarehouseQueryHandler);
		
		ReplenishmentRelationsQueryHandler replenishmentRelationsQueryHandler = new ReplenishmentRelationsQueryHandler(this);
		ret.add(replenishmentRelationsQueryHandler);
		
		SupplyWarehouseStocksQueryHandler supplyWarehouseStocksQueryHandler = new SupplyWarehouseStocksQueryHandler(this);
		ret.add(supplyWarehouseStocksQueryHandler);
		
		ReplenishmentWarehousesQueryHandler replenishmentWarehousesQueryHandler = new ReplenishmentWarehousesQueryHandler(this);
		ret.add(replenishmentWarehousesQueryHandler);
		
		return ret;
	}

}
