/*
 * Copyright 2009-2014 Jagornet Technologies, LLC.  All Rights Reserved.
 *
 * This software is the proprietary information of Jagornet Technologies, LLC. 
 * Use is subject to license terms.
 *
 */

/*
 *   This file GenericOptionFactory.java is part of Jagornet DHCP.
 *
 *   Jagornet DHCP is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Jagornet DHCP is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Jagornet DHCP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.jagornet.dhcp.server.config;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagornet.dhcp.core.option.base.BaseDhcpOption;
import com.jagornet.dhcp.core.option.base.DhcpOption;
import com.jagornet.dhcp.core.option.generic.GenericDomainNameListOption;
import com.jagornet.dhcp.core.option.generic.GenericDomainNameOption;
import com.jagornet.dhcp.core.option.generic.GenericIpAddressListOption;
import com.jagornet.dhcp.core.option.generic.GenericIpAddressOption;
import com.jagornet.dhcp.core.option.generic.GenericOpaqueDataListOption;
import com.jagornet.dhcp.core.option.generic.GenericOpaqueDataOption;
import com.jagornet.dhcp.core.option.generic.GenericStringOption;
import com.jagornet.dhcp.core.option.generic.GenericUnsignedByteOption;
import com.jagornet.dhcp.core.option.generic.GenericUnsignedIntOption;
import com.jagornet.dhcp.core.option.generic.GenericUnsignedShortListOption;
import com.jagornet.dhcp.core.option.generic.GenericUnsignedShortOption;
import com.jagornet.dhcp.server.config.xml.DomainNameListOptionType;
import com.jagornet.dhcp.server.config.xml.DomainNameOptionType;
import com.jagornet.dhcp.server.config.xml.GenericOptionsType;
import com.jagornet.dhcp.server.config.xml.IpAddressListOptionType;
import com.jagornet.dhcp.server.config.xml.IpAddressOptionType;
import com.jagornet.dhcp.server.config.xml.OpaqueData;
import com.jagornet.dhcp.server.config.xml.OpaqueDataListOptionType;
import com.jagornet.dhcp.server.config.xml.OpaqueDataOptionType;
import com.jagornet.dhcp.server.config.xml.OptionDefType;

/**
 * A factory for creating GenericOption objects.
 * 
 * @author A. Gregory Rabil
 */
public class GenericOptionFactory
{
	private static Logger log = LoggerFactory.getLogger(GenericOptionFactory.class);

	public static DhcpOption getDhcpOption(OptionDefType optionDef)
	{
		int code = optionDef.getCode();
		String name = optionDef.getName();
		if (optionDef.getDomainNameListOption() != null) {
			DomainNameListOptionType domainNameListOption = optionDef.getDomainNameListOption();
			GenericDomainNameListOption genericDomainNameListOption = 
					new GenericDomainNameListOption(code, name);
			if (domainNameListOption.getDomainNameList() != null) {
				genericDomainNameListOption.setDomainNameList(domainNameListOption.getDomainNameList());
			}
		}
		else if (optionDef.getDomainNameOption() != null) {
			DomainNameOptionType domainNameOption = optionDef.getDomainNameOption();
			GenericDomainNameOption genericDomainNameOption = 
					new GenericDomainNameOption(code, name);
			if (domainNameOption.getDomainName() != null) {
				genericDomainNameOption.setDomainName(domainNameOption.getDomainName());
			}
		}
		else if (optionDef.getIpAddressListOption() != null) {
			IpAddressListOptionType ipAddressListOption = optionDef.getIpAddressListOption();
			GenericIpAddressListOption genericIpAddressListOption =
					new GenericIpAddressListOption(code, name);
			if (ipAddressListOption.getIpAddressList() != null) {
				genericIpAddressListOption.setIpAddressList(ipAddressListOption.getIpAddressList());
			}
		}
		else if (optionDef.getIpAddressOption() != null) {
			IpAddressOptionType ipAddressOption = optionDef.getIpAddressOption();
			GenericIpAddressOption genericIpAddressOption = 
					new GenericIpAddressOption(code, name);
			if (ipAddressOption.getIpAddress() != null) {
				genericIpAddressOption.setIpAddress(ipAddressOption.getIpAddress());
			}
		}
		else if (optionDef.getOpaqueDataListOption() != null) {
			OpaqueDataListOptionType opaqueDataListOption = optionDef.getOpaqueDataListOption();
			GenericOpaqueDataListOption genericOpaqueDataListOption = 
					new GenericOpaqueDataListOption(code, name);
			if (opaqueDataListOption.getOpaqueDataList() != null) {
        		for (OpaqueData opaqueData : opaqueDataListOption.getOpaqueDataList()) {
        			genericOpaqueDataListOption.addOpaqueData(
        					OpaqueDataUtil.toBaseOpaqueData(opaqueData));
				}
			}
		}
		else if (optionDef.getOpaqueDataOption() != null) {
			OpaqueDataOptionType opaqueDataOption = optionDef.getOpaqueDataOption();
			GenericOpaqueDataOption genericOpaqueDataOption = 
					new GenericOpaqueDataOption(code, name);
			if (opaqueDataOption.getOpaqueData() != null) {
				OpaqueData opaqueData = opaqueDataOption.getOpaqueData();
				genericOpaqueDataOption.setOpaqueData(
						OpaqueDataUtil.toBaseOpaqueData(opaqueData));
			}
		}
		else if (optionDef.getStringOption() != null) {
			return new GenericStringOption(code, name, 
					optionDef.getStringOption().getString());
		}
		else if (optionDef.getUByteOption() != null) {
			return new GenericUnsignedByteOption(code, name, 
					optionDef.getUByteOption().getUnsignedByte());
		}
		else if (optionDef.getUIntOption() != null) {
			return new GenericUnsignedIntOption(code, name, 
					optionDef.getUIntOption().getUnsignedInt());
		}
		else if (optionDef.getUShortListOption() != null) {
			return new GenericUnsignedShortListOption(code, name, 
					optionDef.getUShortListOption().getUnsignedShortList());
		}
		else if (optionDef.getUShortOption() != null) {
			return new GenericUnsignedShortOption(code, name, 
					optionDef.getUShortOption().getUnsignedShort());
		}
		else {
			log.error("Unknown generic option type");
		}
		return null;
	}

	/**
	 * Convert a list of XML Generic options to a map of DhcpOptions
	 * 
	 * @param genericOptions
	 * @return a map of generic options
	 */
	public static Map<Integer, DhcpOption> genericOptions(GenericOptionsType genericOptions) 
	{
		Map<Integer, DhcpOption> optMap = new TreeMap<Integer, DhcpOption>(); 
		if (genericOptions != null) {
			List<OptionDefType> optionDefs = genericOptions.getOptionDefList();
			if ((optionDefs != null) && !optionDefs.isEmpty()) {
				for (OptionDefType optionDefType : optionDefs) {
					
					int code = optionDefType.getCode();
					String name = optionDefType.getName();

/*
					
					// the XML schema defines the optionDefType as a choice,
					// so we must determine which generic option is set
					
					if (optionDefType.getDomainNameListOption() != null) {
						DomainNameListOptionType domainNameListOption =
							optionDefType.getDomainNameListOption();
						if (domainNameListOption != null) {
							GenericDomainNameListOption dhcpOption = 
								new GenericDomainNameListOption(code, name, domainNameListOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}

					if (optionDefType.getDomainNameOption() != null) {
						DomainNameOptionType domainNameOption =
							optionDefType.getDomainNameOption();
						if (domainNameOption != null) {
							GenericDomainNameOption dhcpOption = 
								new GenericDomainNameOption(code, name, domainNameOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}

					if (optionDefType.getIpAddressListOption() != null) {
						IpAddressListOptionType ipAddressListOption =
							optionDefType.getIpAddressListOption();
						if (ipAddressListOption != null) {
							GenericIpAddressListOption dhcpOption = 
								new GenericIpAddressListOption(code, name, ipAddressListOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}

					if (optionDefType.getIpAddressOption() != null) {
						IpAddressOptionType ipAddressOption =
							optionDefType.getIpAddressOption();
						if (ipAddressOption != null) {
							GenericIpAddressOption dhcpOption = 
								new GenericIpAddressOption(code, name, ipAddressOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}

					if (optionDefType.getOpaqueDataListOption() != null) {
						OpaqueDataListOptionType opaqueDataListOption =
							optionDefType.getOpaqueDataListOption();
						if (opaqueDataListOption != null) {
							GenericOpaqueDataListOption dhcpOption = 
								new GenericOpaqueDataListOption(code, name, opaqueDataListOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}

					if (optionDefType.getOpaqueDataOption() != null) {
						OpaqueDataOptionType opaqueDataOption =
							optionDefType.getOpaqueDataOption();
						if (opaqueDataOption != null) {
							GenericOpaqueDataOption dhcpOption = 
								new GenericOpaqueDataOption(code, name, opaqueDataOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}
					
					if (optionDefType.getStringOption() != null) {
						StringOptionType stringOption =
							optionDefType.getStringOption();
						if (stringOption != null) {
							GenericStringOption dhcpOption =
								new GenericStringOption(code, name, stringOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}
					
					if (optionDefType.getUByteListOption() != null) {
						UnsignedByteListOptionType uByteListOption =
							optionDefType.getUByteListOption();
						if (uByteListOption != null) {
							GenericUnsignedByteListOption dhcpOption = 
								new GenericUnsignedByteListOption(code, name, uByteListOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}
					
					if (optionDefType.getUByteOption() != null) {
						UnsignedByteOptionType uByteOption =
							optionDefType.getUByteOption();
						if (uByteOption != null) {
							GenericUnsignedByteOption dhcpOption = 
								new GenericUnsignedByteOption(code, name, uByteOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}
					
					if (optionDefType.getUIntOption() != null) {
						UnsignedIntOptionType uIntOption =
							optionDefType.getUIntOption();
						if (uIntOption != null) {
							GenericUnsignedIntOption dhcpOption = 
								new GenericUnsignedIntOption(code, name, uIntOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}
					
					if (optionDefType.getUShortListOption() != null) {
						UnsignedShortListOptionType uShortListOption =
							optionDefType.getUShortListOption();
						if (uShortListOption != null) {
							GenericUnsignedShortListOption dhcpOption = 
								new GenericUnsignedShortListOption(code, name, uShortListOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}

					if (optionDefType.getUShortOption() != null) {
						UnsignedShortOptionType uShortOption =
							optionDefType.getUShortOption();
						if (uShortOption != null) {
							GenericUnsignedShortOption dhcpOption = 
								new GenericUnsignedShortOption(code, name, uShortOption);
							optMap.put(code, dhcpOption);
							continue;
						}
					}
*/
					
					DhcpOption dhcpOption = getDhcpOption(optionDefType);
					optMap.put(code, dhcpOption);
					
					if(optMap.get(code) instanceof BaseDhcpOption) {
						if (optionDefType.isV4()) {
							((BaseDhcpOption) optMap.get(code)).setV4(true);
						}
					}
				}
			}
		}
		return optMap;
	}

}
