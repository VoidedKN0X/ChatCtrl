/*
Copyright 2022-2024 New Vector Ltd.

SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
Please see LICENSE in the repository root for full details.

ChatCtrl: account sign-up removed. Upstream, this registers new (or guest)
accounts on Element Call's default homeserver, with reCAPTCHA and terms stages.
ChatCtrl only uses Element Call as a widget inside the app, where you're
already signed in, so it never creates accounts or contacts a server for it.
*/

import { useCallback } from "react";
import { type MatrixClient } from "matrix-js-sdk";

import { type Session } from "../ClientContext";

export const useInteractiveRegistration = (
  _oldClient?: MatrixClient,
): {
  privacyPolicyUrl?: string;
  recaptchaKey?: string;
  register: (
    username: string,
    password: string,
    displayName: string,
    recaptchaResponse: string,
    passwordlessUser: boolean,
  ) => Promise<[MatrixClient, Session]>;
} => {
  const register = useCallback(async (): Promise<[MatrixClient, Session]> => {
    throw new Error("Creating accounts isn't available in ChatCtrl");
  }, []);
  return { privacyPolicyUrl: undefined, recaptchaKey: undefined, register };
};
