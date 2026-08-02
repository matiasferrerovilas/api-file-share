package api.m2.file.mappers;

import api.m2.file.entity.AppFileShare;
import api.m2.file.record.FileShareResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppFileShareMapper {

    FileShareResponse toResponse(AppFileShare share);
}
