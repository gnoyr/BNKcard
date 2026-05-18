package com.bnk.domain.notification.mapper;

import com.bnk.domain.notification.model.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    int insertNotification(Notification notification);

    List<Notification> findByUserId(@Param("userId") Long userId);

    int markAsRead(@Param("notificationId") Long notificationId);
}
