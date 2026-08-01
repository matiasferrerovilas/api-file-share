package api.m2.file.record.settings;

import api.m2.file.enums.UserSettingKey;

public record UserSettingResponse(UserSettingKey key, Long value) {
}
