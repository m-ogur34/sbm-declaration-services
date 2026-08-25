package tr.com.allianz.ysv.services.dto.internal;

import tr.com.allianz.ysv.services.entity.DeclarationProcess;
import tr.com.allianz.ysv.services.util.DistrictCodeResolver;

public record DeclarationGroupKey(Integer declarationYear,
                                  Integer declarationMonth,
                                  Integer cityCode,
                                  Integer districtCode) {

    public static DeclarationGroupKey of(DeclarationProcess process) {
        return new DeclarationGroupKey(process.getDeclarationYear(),
                process.getDeclarationMonth(),
                process.getCityCode(),
                DistrictCodeResolver.resolve(process.getDistrictCode()));
    }
}
