import {MenuOption} from "../models/MenuOption";
import {SecurityService} from "../core/security.service";
import {AdminService} from "../core/admin.service";
import {Component} from "@angular/core";

@Component({
	selector: 'sidebar-component',
	template: require('./sidebar.html')
})

export class SidebarComponent {
	menuOptions:MenuOption[];

	constructor(adminService:AdminService, private securityService:SecurityService) {
		this.menuOptions = adminService.getMenuOptions();
	}

	logout() {
		this.securityService.logout();
	}
}
