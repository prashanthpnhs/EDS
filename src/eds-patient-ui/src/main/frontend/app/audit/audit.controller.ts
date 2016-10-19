import {ILoggerService} from "../blocks/logger.service";
import {IAuditService} from "../core/audit.service";

export class AuditController {

		static $inject = ['AuditService', 'LoggerService', '$state'];

		constructor(private auditService:IAuditService,
								private logger:ILoggerService) {
		}
}
